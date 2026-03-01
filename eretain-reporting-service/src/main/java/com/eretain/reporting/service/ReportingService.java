package com.eretain.reporting.service;

import com.eretain.reporting.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private final WebClient projectServiceClient;
    private final WebClient allocationServiceClient;
    private final WebClient timesheetServiceClient;
    private final WebClient authServiceClient;
    private final WebClient companyServiceClient;

    /**
     * Get project report with allocation and timesheet summaries.
     * Available to ADMINISTRATOR and PMO.
     */
    public List<ProjectReportDTO> getProjectReport(ReportFilterDTO filter) {
        log.info("Generating project report with filter: {}", filter);

        try {
            // Fetch projects from project service
            Map<String, Object> projectsResponse = projectServiceClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/projects");
                        if (filter.getStatus() != null) uriBuilder.queryParam("status", filter.getStatus());
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (projectsResponse == null || projectsResponse.get("data") == null) {
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> projects = (List<Map<String, Object>>)
                    ((Map<String, Object>) projectsResponse.get("data")).get("content");

            if (projects == null) return Collections.emptyList();

            return projects.stream().map(project -> {
                Long projectId = Long.valueOf(project.get("id").toString());
                return ProjectReportDTO.builder()
                        .projectId(projectId)
                        .projectName((String) project.get("name"))
                        .projectCode((String) project.get("projectCode"))
                        .status((String) project.get("status"))
                        .billabilityCategory((String) project.get("billabilityCategory"))
                        .clientName((String) project.get("clientName"))
                        .startDate(project.get("startDate") != null ? LocalDate.parse(project.get("startDate").toString()) : null)
                        .endDate(project.get("endDate") != null ? LocalDate.parse(project.get("endDate").toString()) : null)
                        .budget(project.get("budget") != null ? Double.valueOf(project.get("budget").toString()) : null)
                        .currency((String) project.get("currency"))
                        .build();
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error generating project report: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get allocation report for employees.
     * ADMINISTRATOR and PMO can see all; EMPLOYEE can see own allocations only.
     */
    public List<AllocationReportDTO> getAllocationReport(ReportFilterDTO filter) {
        log.info("Generating allocation report with filter: {}", filter);

        try {
            String uri;
            if (filter.getEmployeeId() != null) {
                uri = "/api/allocations/employee/" + filter.getEmployeeId();
            } else if (filter.getProjectId() != null) {
                uri = "/api/allocations/project/" + filter.getProjectId();
            } else {
                // Admin: fetch all allocations
                uri = "/api/allocations";
            }

            Map<String, Object> response = allocationServiceClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null || response.get("data") == null) {
                return Collections.emptyList();
            }

            // Handle both List (by employee) and PagedResponse (all allocations) formats
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allocations;
            Object data = response.get("data");
            if (data instanceof List) {
                allocations = (List<Map<String, Object>>) data;
            } else if (data instanceof Map) {
                Map<String, Object> pagedData = (Map<String, Object>) data;
                allocations = pagedData.get("content") != null
                        ? (List<Map<String, Object>>) pagedData.get("content")
                        : Collections.emptyList();
            } else {
                allocations = Collections.emptyList();
            }

            // Build lookup maps
            Map<Long, String> projectNameMap = buildProjectNameMap();
            Map<Long, String> employeeNameMap = buildEmployeeNameMap();

            return allocations.stream()
                    .filter(a -> filterAllocation(a, filter))
                    .map(allocation -> {
                        Double hoursPerDay = allocation.get("hoursPerDay") != null
                                ? Double.valueOf(allocation.get("hoursPerDay").toString()) : 0.0;
                        LocalDate start = allocation.get("startDate") != null
                                ? LocalDate.parse(allocation.get("startDate").toString()) : null;
                        LocalDate end = allocation.get("endDate") != null
                                ? LocalDate.parse(allocation.get("endDate").toString()) : null;

                        long workDays = start != null && end != null ? calculateWorkDays(start, end) : 0;
                        double totalAllocated = hoursPerDay * workDays;

                        Long projectId = allocation.get("projectId") != null
                                ? Long.valueOf(allocation.get("projectId").toString()) : null;
                        Long employeeId = allocation.get("employeeId") != null
                                ? Long.valueOf(allocation.get("employeeId").toString()) : null;

                        return AllocationReportDTO.builder()
                                .employeeId(employeeId)
                                .employeeName(employeeId != null
                                        ? employeeNameMap.getOrDefault(employeeId, "Employee #" + employeeId) : null)
                                .projectId(projectId)
                                .projectName(projectId != null
                                        ? projectNameMap.getOrDefault(projectId, "Project #" + projectId) : null)
                                .roleName((String) allocation.get("roleName"))
                                .allocationStatus((String) allocation.get("status"))
                                .startDate(start)
                                .endDate(end)
                                .hoursPerDay(hoursPerDay)
                                .totalAllocatedHours(totalAllocated)
                                .utilizationPercentage(workDays > 0 ? (totalAllocated / (workDays * 8.0)) * 100 : 0)
                                .build();
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error generating allocation report: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Build a projectId to projectName lookup map by fetching all projects.
     */
    @SuppressWarnings("unchecked")
    private Map<Long, String> buildProjectNameMap() {
        Map<Long, String> map = new HashMap<>();
        try {
            Map<String, Object> projectsResponse = projectServiceClient.get()
                    .uri("/api/projects?page=0&size=200")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (projectsResponse != null && projectsResponse.get("data") != null) {
                Map<String, Object> data = (Map<String, Object>) projectsResponse.get("data");
                if (data.get("content") != null) {
                    List<Map<String, Object>> projects = (List<Map<String, Object>>) data.get("content");
                    for (Map<String, Object> p : projects) {
                        Long id = p.get("id") != null ? Long.valueOf(p.get("id").toString()) : null;
                        String name = (String) p.get("name");
                        if (name == null) name = (String) p.get("projectName");
                        if (id != null && name != null) {
                            map.put(id, name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch project names: {}", e.getMessage());
        }
        return map;
    }

    /**
     * Build an employeeId to employee name lookup map by fetching from auth service.
     */
    @SuppressWarnings("unchecked")
    private Map<Long, String> buildEmployeeNameMap() {
        Map<Long, String> map = new HashMap<>();
        try {
            Map<String, Object> response = authServiceClient.get()
                    .uri("/api/auth/users/names")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response != null && response.get("data") != null) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    try {
                        Long id = Long.valueOf(entry.getKey());
                        String name = entry.getValue() != null ? entry.getValue().toString() : null;
                        if (name != null) {
                            map.put(id, name);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch employee names: {}", e.getMessage());
        }
        return map;
    }

    /**
     * Get timesheet summary report.
     */
    public List<TimesheetReportDTO> getTimesheetReport(ReportFilterDTO filter) {
        log.info("Generating timesheet report with filter: {}", filter);

        try {
            String uri = filter.getEmployeeId() != null
                    ? "/api/timesheets/employee/" + filter.getEmployeeId()
                    : "/api/timesheets";

            Map<String, Object> response = timesheetServiceClient.get()
                    .uri(uri + "?page=0&size=200")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null || response.get("data") == null) {
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> pagedData = (Map<String, Object>) response.get("data");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> timesheets = (List<Map<String, Object>>) pagedData.get("content");

            if (timesheets == null) return Collections.emptyList();

            Map<Long, String> employeeNameMap = buildEmployeeNameMap();

            return timesheets.stream()
                    .filter(t -> filterTimesheet(t, filter))
                    .map(ts -> {
                            Long empId = ts.get("employeeId") != null
                                    ? Long.valueOf(ts.get("employeeId").toString()) : null;
                            return TimesheetReportDTO.builder()
                            .id(ts.get("id") != null
                                    ? Long.valueOf(ts.get("id").toString()) : null)
                            .employeeId(empId)
                            .employeeName(empId != null
                                    ? employeeNameMap.getOrDefault(empId, "Employee #" + empId) : null)
                            .weekStartDate(ts.get("weekStartDate") != null
                                    ? LocalDate.parse(ts.get("weekStartDate").toString()) : null)
                            .weekEndDate(ts.get("weekEndDate") != null
                                    ? LocalDate.parse(ts.get("weekEndDate").toString()) : null)
                            .status((String) ts.get("status"))
                            .totalHours(ts.get("totalHours") != null
                                    ? Double.valueOf(ts.get("totalHours").toString()) : 0.0)
                            .submittedDate(ts.get("submittedDate") != null
                                    ? LocalDate.parse(ts.get("submittedDate").toString()) : null)
                            .build();
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error generating timesheet report: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get employee utilization report.
     */
    public EmployeeUtilizationDTO getEmployeeUtilization(Long employeeId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating utilization report for employee: {} from {} to {}", employeeId, startDate, endDate);

        List<AllocationReportDTO> allocations = getAllocationReport(
                ReportFilterDTO.builder().employeeId(employeeId).startDate(startDate).endDate(endDate).build());

        long totalWorkDays = calculateWorkDays(startDate, endDate);
        double totalAvailableHours = totalWorkDays * 8.0;
        double totalAllocatedHours = allocations.stream()
                .mapToDouble(a -> a.getTotalAllocatedHours() != null ? a.getTotalAllocatedHours() : 0)
                .sum();

        int activeProjects = (int) allocations.stream()
                .filter(a -> "ACTIVE".equals(a.getAllocationStatus()))
                .map(AllocationReportDTO::getProjectId)
                .distinct()
                .count();

        return EmployeeUtilizationDTO.builder()
                .employeeId(employeeId)
                .totalAvailableHours(totalAvailableHours)
                .totalAllocatedHours(totalAllocatedHours)
                .allocationUtilization(totalAvailableHours > 0
                        ? (totalAllocatedHours / totalAvailableHours) * 100 : 0)
                .activeProjects(activeProjects)
                .allocations(allocations)
                .build();
    }

    // =================== Hierarchical Utilization ===================

    /**
     * Get utilization overview at all Business Unit levels.
     */
    public List<UtilizationSummaryDTO> getUtilizationOverview(LocalDate startDate, LocalDate endDate) {
        log.info("Generating utilization overview from {} to {}", startDate, endDate);

        // Fetch all data upfront
        List<Map<String, Object>> allBusinessUnits = fetchBusinessUnits();
        List<Map<String, Object>> allProjects = fetchAllProjects();
        List<Map<String, Object>> allAllocations = fetchAllAllocationsFlat();
        Map<Long, String> employeeNameMap = buildEmployeeNameMap();
        Map<Long, String> employeeDesignationMap = buildEmployeeDesignationMap();
        long workDays = calculateWorkDays(startDate, endDate);

        // Build project → deliveryUnitId map
        Map<Long, Long> projectToDeliveryUnit = new HashMap<>();
        for (Map<String, Object> p : allProjects) {
            Long pid = toLong(p.get("id"));
            Long duId = toLong(p.get("deliveryUnitId"));
            if (pid != null && duId != null) {
                projectToDeliveryUnit.put(pid, duId);
            }
        }

        // Build allocation summaries per project
        Map<Long, List<Map<String, Object>>> allocationsByProject = new HashMap<>();
        for (Map<String, Object> a : allAllocations) {
            Long projectId = toLong(a.get("projectId"));
            if (projectId != null) {
                allocationsByProject.computeIfAbsent(projectId, k -> new ArrayList<>()).add(a);
            }
        }

        List<UtilizationSummaryDTO> result = new ArrayList<>();
        for (Map<String, Object> bu : allBusinessUnits) {
            result.add(buildBusinessUnitSummary(bu, allProjects, projectToDeliveryUnit,
                    allocationsByProject, employeeNameMap, workDays, startDate, endDate));
        }
        return result;
    }

    /**
     * Get utilization for a specific level: unit, delivery-unit, project, or employee.
     */
    public UtilizationSummaryDTO getUtilizationByLevel(String level, Long id,
                                                        LocalDate startDate, LocalDate endDate) {
        log.info("Generating utilization for {} id={} from {} to {}", level, id, startDate, endDate);

        List<Map<String, Object>> allProjects = fetchAllProjects();
        List<Map<String, Object>> allAllocations = fetchAllAllocationsFlat();
        Map<Long, String> employeeNameMap = buildEmployeeNameMap();
        Map<Long, String> employeeDesignationMap = buildEmployeeDesignationMap();
        long workDays = calculateWorkDays(startDate, endDate);

        Map<Long, Long> projectToDeliveryUnit = new HashMap<>();
        for (Map<String, Object> p : allProjects) {
            Long pid = toLong(p.get("id"));
            Long duId = toLong(p.get("deliveryUnitId"));
            if (pid != null && duId != null) projectToDeliveryUnit.put(pid, duId);
        }

        Map<Long, List<Map<String, Object>>> allocationsByProject = new HashMap<>();
        for (Map<String, Object> a : allAllocations) {
            Long projectId = toLong(a.get("projectId"));
            if (projectId != null) allocationsByProject.computeIfAbsent(projectId, k -> new ArrayList<>()).add(a);
        }

        switch (level.toUpperCase()) {
            case "BUSINESS_UNIT" -> {
                List<Map<String, Object>> bus = fetchBusinessUnits();
                Map<String, Object> bu = bus.stream()
                        .filter(b -> id.equals(toLong(b.get("id"))))
                        .findFirst().orElse(null);
                if (bu == null) return null;
                return buildBusinessUnitSummary(bu, allProjects, projectToDeliveryUnit,
                        allocationsByProject, employeeNameMap, workDays, startDate, endDate);
            }
            case "UNIT" -> {
                List<Map<String, Object>> allUnits = fetchUnits();
                Map<String, Object> unit = allUnits.stream()
                        .filter(u -> id.equals(toLong(u.get("id"))))
                        .findFirst().orElse(null);
                if (unit == null) return null;
                return buildUnitSummary(unit, allProjects, projectToDeliveryUnit,
                        allocationsByProject, employeeNameMap, workDays, startDate, endDate);
            }
            case "DELIVERY_UNIT" -> {
                List<Map<String, Object>> allDUs = fetchDeliveryUnits();
                Map<String, Object> du = allDUs.stream()
                        .filter(d -> id.equals(toLong(d.get("id"))))
                        .findFirst().orElse(null);
                if (du == null) return null;
                return buildDeliveryUnitSummary(du, allProjects, projectToDeliveryUnit,
                        allocationsByProject, employeeNameMap, workDays, startDate, endDate);
            }
            case "PROJECT" -> {
                Map<String, Object> project = allProjects.stream()
                        .filter(p -> id.equals(toLong(p.get("id"))))
                        .findFirst().orElse(null);
                if (project == null) return null;
                return buildProjectSummary(project, allocationsByProject, employeeNameMap,
                        employeeDesignationMap, workDays, startDate, endDate);
            }
            case "EMPLOYEE" -> {
                return buildEmployeeSummary(id, allAllocations, allProjects, employeeNameMap,
                        employeeDesignationMap, workDays, startDate, endDate);
            }
            default -> { return null; }
        }
    }

    @SuppressWarnings("unchecked")
    private UtilizationSummaryDTO buildBusinessUnitSummary(
            Map<String, Object> bu,
            List<Map<String, Object>> allProjects,
            Map<Long, Long> projectToDeliveryUnit,
            Map<Long, List<Map<String, Object>>> allocationsByProject,
            Map<Long, String> employeeNameMap,
            long workDays, LocalDate startDate, LocalDate endDate) {

        Long buId = toLong(bu.get("id"));
        List<Map<String, Object>> units = bu.get("units") instanceof List
                ? (List<Map<String, Object>>) bu.get("units") : Collections.emptyList();

        // Collect all delivery unit IDs under this BU
        Set<Long> buDeliveryUnitIds = new HashSet<>();
        for (Map<String, Object> u : units) {
            List<Map<String, Object>> dus = u.get("deliveryUnits") instanceof List
                    ? (List<Map<String, Object>>) u.get("deliveryUnits") : Collections.emptyList();
            for (Map<String, Object> d : dus) {
                Long did = toLong(d.get("id"));
                if (did != null) buDeliveryUnitIds.add(did);
            }
        }

        // Collect employees and projects from allocations in this BU's projects
        Set<Long> allEmployees = new HashSet<>();
        Set<Long> allActiveProjects = new HashSet<>();
        for (Map<String, Object> project : allProjects) {
            Long duId = toLong(project.get("deliveryUnitId"));
            if (duId != null && buDeliveryUnitIds.contains(duId)) {
                Long projectId = toLong(project.get("id"));
                if ("ACTIVE".equals(project.get("status"))) allActiveProjects.add(projectId);
                for (Map<String, Object> a : allocationsByProject.getOrDefault(projectId, Collections.emptyList())) {
                    Long empId = toLong(a.get("employeeId"));
                    if (empId != null) allEmployees.add(empId);
                }
            }
        }

        List<UtilizationSummaryDTO> childSummaries = new ArrayList<>();
        double totalAllocated = 0;
        int totalAllocs = 0;

        for (Map<String, Object> unit : units) {
            UtilizationSummaryDTO unitSummary = buildUnitSummary(unit, allProjects,
                    projectToDeliveryUnit, allocationsByProject, employeeNameMap, workDays, startDate, endDate);
            childSummaries.add(unitSummary);
            totalAllocated += unitSummary.getTotalAllocatedHours() != null ? unitSummary.getTotalAllocatedHours() : 0;
            totalAllocs += unitSummary.getTotalAllocations() != null ? unitSummary.getTotalAllocations() : 0;
        }

        double totalAvailable = allEmployees.size() * workDays * 8.0;

        return UtilizationSummaryDTO.builder()
                .id(buId)
                .name((String) bu.get("name"))
                .code((String) bu.get("code"))
                .level("BUSINESS_UNIT")
                .headCount(allEmployees.size())
                .totalAvailableHours(totalAvailable)
                .totalAllocatedHours(totalAllocated)
                .utilizationPercentage(totalAvailable > 0 ? (totalAllocated / totalAvailable) * 100 : 0)
                .activeProjects(allActiveProjects.size())
                .totalAllocations(totalAllocs)
                .children(childSummaries)
                .build();
    }

    @SuppressWarnings("unchecked")
    private UtilizationSummaryDTO buildUnitSummary(
            Map<String, Object> unit,
            List<Map<String, Object>> allProjects,
            Map<Long, Long> projectToDeliveryUnit,
            Map<Long, List<Map<String, Object>>> allocationsByProject,
            Map<Long, String> employeeNameMap,
            long workDays, LocalDate startDate, LocalDate endDate) {

        Long unitId = toLong(unit.get("id"));
        List<Map<String, Object>> deliveryUnits = unit.get("deliveryUnits") instanceof List
                ? (List<Map<String, Object>>) unit.get("deliveryUnits") : Collections.emptyList();

        // Collect DU IDs under this unit
        Set<Long> unitDuIds = new HashSet<>();
        for (Map<String, Object> d : deliveryUnits) {
            Long did = toLong(d.get("id"));
            if (did != null) unitDuIds.add(did);
        }

        // Collect employees from allocations in this unit's projects
        Set<Long> allEmployees = new HashSet<>();
        Set<Long> allActiveProjects = new HashSet<>();
        for (Map<String, Object> project : allProjects) {
            Long duId = toLong(project.get("deliveryUnitId"));
            if (duId != null && unitDuIds.contains(duId)) {
                Long projectId = toLong(project.get("id"));
                if ("ACTIVE".equals(project.get("status"))) allActiveProjects.add(projectId);
                for (Map<String, Object> a : allocationsByProject.getOrDefault(projectId, Collections.emptyList())) {
                    Long empId = toLong(a.get("employeeId"));
                    if (empId != null) allEmployees.add(empId);
                }
            }
        }

        List<UtilizationSummaryDTO> childSummaries = new ArrayList<>();
        double totalAllocated = 0;
        int totalAllocs = 0;

        for (Map<String, Object> du : deliveryUnits) {
            UtilizationSummaryDTO duSummary = buildDeliveryUnitSummary(du, allProjects,
                    projectToDeliveryUnit, allocationsByProject, employeeNameMap, workDays, startDate, endDate);
            childSummaries.add(duSummary);
            totalAllocated += duSummary.getTotalAllocatedHours() != null ? duSummary.getTotalAllocatedHours() : 0;
            totalAllocs += duSummary.getTotalAllocations() != null ? duSummary.getTotalAllocations() : 0;
        }

        double totalAvailable = allEmployees.size() * workDays * 8.0;

        return UtilizationSummaryDTO.builder()
                .id(unitId)
                .name((String) unit.get("name"))
                .code((String) unit.get("code"))
                .level("UNIT")
                .headCount(allEmployees.size())
                .totalAvailableHours(totalAvailable)
                .totalAllocatedHours(totalAllocated)
                .utilizationPercentage(totalAvailable > 0 ? (totalAllocated / totalAvailable) * 100 : 0)
                .activeProjects(allActiveProjects.size())
                .totalAllocations(totalAllocs)
                .children(childSummaries)
                .build();
    }

    private UtilizationSummaryDTO buildDeliveryUnitSummary(
            Map<String, Object> du,
            List<Map<String, Object>> allProjects,
            Map<Long, Long> projectToDeliveryUnit,
            Map<Long, List<Map<String, Object>>> allocationsByProject,
            Map<Long, String> employeeNameMap,
            long workDays, LocalDate startDate, LocalDate endDate) {

        Long duId = toLong(du.get("id"));

        // Find projects in this delivery unit
        List<Map<String, Object>> duProjects = allProjects.stream()
                .filter(p -> duId.equals(toLong(p.get("deliveryUnitId"))))
                .collect(Collectors.toList());

        List<UtilizationSummaryDTO> childSummaries = new ArrayList<>();
        double totalAllocated = 0;
        Set<Long> allEmployees = new HashSet<>();
        Set<Long> activeProjectIds = new HashSet<>();
        int totalAllocs = 0;

        for (Map<String, Object> project : duProjects) {
            Long projectId = toLong(project.get("id"));
            List<Map<String, Object>> projAllocations = allocationsByProject.getOrDefault(projectId, Collections.emptyList());

            String status = (String) project.get("status");
            if ("ACTIVE".equals(status)) {
                activeProjectIds.add(projectId);
            }

            double projAllocated = 0;
            Set<Long> projEmployees = new HashSet<>();
            for (Map<String, Object> a : projAllocations) {
                Double hpd = toDouble(a.get("hoursPerDay"));
                LocalDate aStart = parseDate(a.get("startDate"));
                LocalDate aEnd = parseDate(a.get("endDate"));
                // Overlap period with requested date range
                LocalDate effectiveStart = aStart != null && aStart.isAfter(startDate) ? aStart : startDate;
                LocalDate effectiveEnd = aEnd != null && aEnd.isBefore(endDate) ? aEnd : endDate;
                if (!effectiveStart.isAfter(effectiveEnd)) {
                    long allocWorkDays = calculateWorkDays(effectiveStart, effectiveEnd);
                    projAllocated += hpd * allocWorkDays;
                }
                Long empId = toLong(a.get("employeeId"));
                if (empId != null) projEmployees.add(empId);
                totalAllocs++;
            }

            allEmployees.addAll(projEmployees);
            totalAllocated += projAllocated;

            childSummaries.add(UtilizationSummaryDTO.builder()
                    .id(projectId)
                    .name((String) project.get("name"))
                    .code((String) project.get("projectCode"))
                    .level("PROJECT")
                    .headCount(projEmployees.size())
                    .totalAvailableHours(projEmployees.size() * workDays * 8.0)
                    .totalAllocatedHours(projAllocated)
                    .utilizationPercentage(projEmployees.size() > 0 && workDays > 0
                            ? (projAllocated / (projEmployees.size() * workDays * 8.0)) * 100 : 0)
                    .activeProjects("ACTIVE".equals(status) ? 1 : 0)
                    .totalAllocations(projAllocations.size())
                    .build());
        }

        double totalAvailable = allEmployees.size() * workDays * 8.0;

        return UtilizationSummaryDTO.builder()
                .id(duId)
                .name((String) du.get("name"))
                .code((String) du.get("code"))
                .level("DELIVERY_UNIT")
                .headCount(allEmployees.size())
                .totalAvailableHours(totalAvailable)
                .totalAllocatedHours(totalAllocated)
                .utilizationPercentage(totalAvailable > 0 ? (totalAllocated / totalAvailable) * 100 : 0)
                .activeProjects(activeProjectIds.size())
                .totalAllocations(totalAllocs)
                .children(childSummaries)
                .build();
    }

    private UtilizationSummaryDTO buildProjectSummary(
            Map<String, Object> project,
            Map<Long, List<Map<String, Object>>> allocationsByProject,
            Map<Long, String> employeeNameMap,
            Map<Long, String> employeeDesignationMap,
            long workDays, LocalDate startDate, LocalDate endDate) {

        Long projectId = toLong(project.get("id"));
        List<Map<String, Object>> projAllocations = allocationsByProject.getOrDefault(projectId, Collections.emptyList());

        // Group allocations by employee
        Map<Long, List<Map<String, Object>>> allocsByEmployee = new HashMap<>();
        for (Map<String, Object> a : projAllocations) {
            Long empId = toLong(a.get("employeeId"));
            if (empId != null) allocsByEmployee.computeIfAbsent(empId, k -> new ArrayList<>()).add(a);
        }

        List<UtilizationSummaryDTO> employeeSummaries = new ArrayList<>();
        double totalAllocated = 0;

        for (Map.Entry<Long, List<Map<String, Object>>> entry : allocsByEmployee.entrySet()) {
            Long empId = entry.getKey();
            double empAllocated = 0;
            for (Map<String, Object> a : entry.getValue()) {
                Double hpd = toDouble(a.get("hoursPerDay"));
                LocalDate aStart = parseDate(a.get("startDate"));
                LocalDate aEnd = parseDate(a.get("endDate"));
                LocalDate effectiveStart = aStart != null && aStart.isAfter(startDate) ? aStart : startDate;
                LocalDate effectiveEnd = aEnd != null && aEnd.isBefore(endDate) ? aEnd : endDate;
                if (!effectiveStart.isAfter(effectiveEnd)) {
                    empAllocated += hpd * calculateWorkDays(effectiveStart, effectiveEnd);
                }
            }
            double empAvailable = workDays * 8.0;
            totalAllocated += empAllocated;

            String empName = employeeNameMap.getOrDefault(empId, "Employee #" + empId);
            String designation = employeeDesignationMap.getOrDefault(empId, "");

            employeeSummaries.add(UtilizationSummaryDTO.builder()
                    .id(empId)
                    .name(empName)
                    .code(designation)
                    .level("EMPLOYEE")
                    .headCount(1)
                    .totalAvailableHours(empAvailable)
                    .totalAllocatedHours(empAllocated)
                    .utilizationPercentage(empAvailable > 0 ? (empAllocated / empAvailable) * 100 : 0)
                    .activeProjects(1)
                    .totalAllocations(entry.getValue().size())
                    .build());
        }

        double totalAvailable = allocsByEmployee.size() * workDays * 8.0;

        return UtilizationSummaryDTO.builder()
                .id(projectId)
                .name((String) project.get("name"))
                .code((String) project.get("projectCode"))
                .level("PROJECT")
                .headCount(allocsByEmployee.size())
                .totalAvailableHours(totalAvailable)
                .totalAllocatedHours(totalAllocated)
                .utilizationPercentage(totalAvailable > 0 ? (totalAllocated / totalAvailable) * 100 : 0)
                .activeProjects("ACTIVE".equals(project.get("status")) ? 1 : 0)
                .totalAllocations(projAllocations.size())
                .children(employeeSummaries)
                .build();
    }

    private UtilizationSummaryDTO buildEmployeeSummary(
            Long employeeId,
            List<Map<String, Object>> allAllocations,
            List<Map<String, Object>> allProjects,
            Map<Long, String> employeeNameMap,
            Map<Long, String> employeeDesignationMap,
            long workDays, LocalDate startDate, LocalDate endDate) {

        // Get all allocations for this employee
        List<Map<String, Object>> empAllocations = allAllocations.stream()
                .filter(a -> employeeId.equals(toLong(a.get("employeeId"))))
                .collect(Collectors.toList());

        Map<Long, String> projectNameMap = allProjects.stream()
                .collect(Collectors.toMap(
                        p -> toLong(p.get("id")),
                        p -> (String) p.getOrDefault("name", "Unknown"),
                        (a, b) -> a));

        // Build per-project child summaries
        Map<Long, List<Map<String, Object>>> byProject = new HashMap<>();
        for (Map<String, Object> a : empAllocations) {
            Long pid = toLong(a.get("projectId"));
            if (pid != null) byProject.computeIfAbsent(pid, k -> new ArrayList<>()).add(a);
        }

        List<UtilizationSummaryDTO> projectSummaries = new ArrayList<>();
        double totalAllocated = 0;
        Set<Long> activeProjectIds = new HashSet<>();

        for (Map.Entry<Long, List<Map<String, Object>>> entry : byProject.entrySet()) {
            Long pid = entry.getKey();
            double projAllocated = 0;
            for (Map<String, Object> a : entry.getValue()) {
                Double hpd = toDouble(a.get("hoursPerDay"));
                LocalDate aStart = parseDate(a.get("startDate"));
                LocalDate aEnd = parseDate(a.get("endDate"));
                LocalDate effectiveStart = aStart != null && aStart.isAfter(startDate) ? aStart : startDate;
                LocalDate effectiveEnd = aEnd != null && aEnd.isBefore(endDate) ? aEnd : endDate;
                if (!effectiveStart.isAfter(effectiveEnd)) {
                    projAllocated += hpd * calculateWorkDays(effectiveStart, effectiveEnd);
                }
            }
            totalAllocated += projAllocated;

            String allocStatus = entry.getValue().stream()
                    .map(a -> (String) a.get("status"))
                    .filter("ACTIVE"::equals)
                    .findFirst().orElse(null);
            if (allocStatus != null) activeProjectIds.add(pid);

            projectSummaries.add(UtilizationSummaryDTO.builder()
                    .id(pid)
                    .name(projectNameMap.getOrDefault(pid, "Project #" + pid))
                    .level("PROJECT")
                    .headCount(1)
                    .totalAvailableHours(workDays * 8.0)
                    .totalAllocatedHours(projAllocated)
                    .utilizationPercentage(workDays > 0 ? (projAllocated / (workDays * 8.0)) * 100 : 0)
                    .totalAllocations(entry.getValue().size())
                    .build());
        }

        double totalAvailable = workDays * 8.0;

        return UtilizationSummaryDTO.builder()
                .id(employeeId)
                .name(employeeNameMap.getOrDefault(employeeId, "Employee #" + employeeId))
                .code(employeeDesignationMap.getOrDefault(employeeId, ""))
                .level("EMPLOYEE")
                .headCount(1)
                .totalAvailableHours(totalAvailable)
                .totalAllocatedHours(totalAllocated)
                .utilizationPercentage(totalAvailable > 0 ? (totalAllocated / totalAvailable) * 100 : 0)
                .activeProjects(activeProjectIds.size())
                .totalAllocations(empAllocations.size())
                .children(projectSummaries)
                .build();
    }

    // =================== Data Fetchers ===================

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchBusinessUnits() {
        try {
            Map<String, Object> response = companyServiceClient.get()
                    .uri("/api/company/business-units")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            if (response != null && response.get("data") instanceof List) {
                return (List<Map<String, Object>>) response.get("data");
            }
        } catch (Exception e) {
            log.warn("Could not fetch business units: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchUnits() {
        try {
            Map<String, Object> response = companyServiceClient.get()
                    .uri("/api/company/units")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            if (response != null && response.get("data") instanceof List) {
                return (List<Map<String, Object>>) response.get("data");
            }
        } catch (Exception e) {
            log.warn("Could not fetch units: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchDeliveryUnits() {
        try {
            Map<String, Object> response = companyServiceClient.get()
                    .uri("/api/company/delivery-units")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            if (response != null && response.get("data") instanceof List) {
                return (List<Map<String, Object>>) response.get("data");
            }
        } catch (Exception e) {
            log.warn("Could not fetch delivery units: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchAllProjects() {
        List<Map<String, Object>> all = new ArrayList<>();
        try {
            Map<String, Object> response = projectServiceClient.get()
                    .uri("/api/projects?page=0&size=500")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            if (response != null && response.get("data") instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data.get("content") instanceof List) {
                    all = (List<Map<String, Object>>) data.get("content");
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch projects: {}", e.getMessage());
        }
        return all;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchAllAllocationsFlat() {
        List<Map<String, Object>> all = new ArrayList<>();
        try {
            Map<String, Object> response = allocationServiceClient.get()
                    .uri("/api/allocations?page=0&size=1000")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            if (response != null && response.get("data") != null) {
                Object data = response.get("data");
                if (data instanceof Map) {
                    Map<String, Object> pagedData = (Map<String, Object>) data;
                    if (pagedData.get("content") instanceof List) {
                        all = (List<Map<String, Object>>) pagedData.get("content");
                    }
                } else if (data instanceof List) {
                    all = (List<Map<String, Object>>) data;
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch allocations: {}", e.getMessage());
        }
        return all;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, String> buildEmployeeDesignationMap() {
        Map<Long, String> map = new HashMap<>();
        try {
            Map<String, Object> response = authServiceClient.get()
                    .uri("/api/auth/users?page=0&size=500")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
            if (response != null && response.get("data") instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data.get("content") instanceof List) {
                    List<Map<String, Object>> users = (List<Map<String, Object>>) data.get("content");
                    for (Map<String, Object> u : users) {
                        Long id = toLong(u.get("id"));
                        String designation = (String) u.get("designation");
                        if (id != null) map.put(id, designation != null ? designation : "");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch employee designations: {}", e.getMessage());
        }
        return map;
    }

    // =================== Utility Helpers ===================

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.valueOf(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.valueOf(val.toString()); } catch (NumberFormatException e) { return 0.0; }
    }

    private LocalDate parseDate(Object val) {
        if (val == null) return null;
        try { return LocalDate.parse(val.toString()); } catch (Exception e) { return null; }
    }

    // =================== Helper Methods ===================

    private boolean filterAllocation(Map<String, Object> allocation, ReportFilterDTO filter) {
        if (filter.getStatus() != null && !filter.getStatus().equals(allocation.get("status"))) {
            return false;
        }
        if (filter.getStartDate() != null && allocation.get("startDate") != null) {
            LocalDate allocStart = LocalDate.parse(allocation.get("startDate").toString());
            if (allocStart.isBefore(filter.getStartDate())) return false;
        }
        if (filter.getEndDate() != null && allocation.get("endDate") != null) {
            LocalDate allocEnd = LocalDate.parse(allocation.get("endDate").toString());
            if (allocEnd.isAfter(filter.getEndDate())) return false;
        }
        return true;
    }

    private boolean filterTimesheet(Map<String, Object> timesheet, ReportFilterDTO filter) {
        if (filter.getStatus() != null && !filter.getStatus().equals(timesheet.get("status"))) {
            return false;
        }
        if (filter.getStartDate() != null && timesheet.get("weekStartDate") != null) {
            LocalDate wsDate = LocalDate.parse(timesheet.get("weekStartDate").toString());
            if (wsDate.isBefore(filter.getStartDate())) return false;
        }
        if (filter.getEndDate() != null && timesheet.get("weekEndDate") != null) {
            LocalDate weDate = LocalDate.parse(timesheet.get("weekEndDate").toString());
            if (weDate.isAfter(filter.getEndDate())) return false;
        }
        return true;
    }

    private long calculateWorkDays(LocalDate start, LocalDate end) {
        long days = 0;
        LocalDate current = start;
        while (!current.isAfter(end)) {
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY && current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                days++;
            }
            current = current.plusDays(1);
        }
        return days;
    }
}
