package com.eretain.ui.controller;

import com.eretain.ui.security.UserPrincipal;
import com.eretain.ui.service.ApiService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/timesheets")
public class TimesheetController {

    private final ApiService apiService;

    public TimesheetController(ApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping
    @SuppressWarnings("unchecked")
    public String listTimesheets(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 Authentication authentication,
                                 HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        boolean isAdminOrPmo = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))
                || authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PMO"));

        try {
            String url;
            if (isAdminOrPmo) {
                url = "/api/timesheets?page=" + page + "&size=" + size;
            } else {
                url = "/api/timesheets/my-timesheets?page=" + page + "&size=" + size;
            }
            Map<String, Object> result = apiService.get(url, token);
            if (apiService.isSuccess(result)) {
                Object data = apiService.extractData(result);
                if (data instanceof Map) {
                    Map<String, Object> pagedData = (Map<String, Object>) data;
                    List<Map<String, Object>> timesheets = (List<Map<String, Object>>) pagedData.get("content");
                    // Enrich with employee names if admin/PMO
                    if (isAdminOrPmo && timesheets != null) {
                        enrichTimesheetsWithEmployeeNames(timesheets, token);
                    }
                    model.addAttribute("timesheets", timesheets);
                    model.addAttribute("currentPage", page);
                    model.addAttribute("totalPages", pagedData.get("totalPages"));
                    model.addAttribute("totalElements", pagedData.get("totalElements"));
                } else {
                    model.addAttribute("timesheets", data);
                }
            }
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load timesheets");
        }
        model.addAttribute("isAdminOrPmo", isAdminOrPmo);
        return "timesheets/list";
    }

    @GetMapping("/{id}")
    @SuppressWarnings("unchecked")
    public String viewTimesheet(@PathVariable Long id,
                                Authentication authentication,
                                HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        boolean isAdminOrPmo = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))
                || authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PMO"));

        try {
            Map<String, Object> result = apiService.get("/api/timesheets/" + id, token);
            if (apiService.isSuccess(result)) {
                Map<String, Object> timesheet = (Map<String, Object>) apiService.extractData(result);
                // Enrich entries with project names
                enrichTimesheetEntriesWithProjectNames(timesheet, token);
                model.addAttribute("timesheet", timesheet);
                model.addAttribute("isAdminOrPmo", isAdminOrPmo);
                return "timesheets/view";
            }
        } catch (Exception e) {
            // fall through
        }
        return "redirect:/timesheets";
    }

    @GetMapping("/new")
    @SuppressWarnings("unchecked")
    public String newTimesheet(Authentication authentication,
                               HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Long userId = principal.getUserId();

        model.addAttribute("timesheet", new HashMap<String, Object>());
        model.addAttribute("isEdit", false);
        model.addAttribute("employeeId", userId);
        loadAllocationsForEmployee(userId, token, model);
        return "timesheets/form";
    }

    @GetMapping("/{id}/edit")
    @SuppressWarnings("unchecked")
    public String editTimesheet(@PathVariable Long id,
                                Authentication authentication,
                                HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Long userId = principal.getUserId();

        try {
            Map<String, Object> result = apiService.get("/api/timesheets/" + id, token);
            if (apiService.isSuccess(result)) {
                Map<String, Object> ts = (Map<String, Object>) apiService.extractData(result);
                // Only allow editing DRAFT or REJECTED timesheets
                String status = (String) ts.get("status");
                if ("DRAFT".equals(status) || "REJECTED".equals(status)) {
                    model.addAttribute("timesheet", ts);
                    model.addAttribute("isEdit", true);
                    model.addAttribute("employeeId", userId);
                    loadAllocationsForEmployee(userId, token, model);
                    return "timesheets/form";
                }
            }
        } catch (Exception e) {
            // fall through
        }
        return "redirect:/timesheets";
    }

    @PostMapping
    public String saveTimesheet(@RequestParam Map<String, String> formData,
                                Authentication authentication,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Map<String, Object> payload = buildTimesheetPayload(formData, principal.getUserId());

        try {
            String id = formData.get("id");
            Map<String, Object> result;
            if (id != null && !id.isEmpty()) {
                result = apiService.put("/api/timesheets/" + id, payload, token);
            } else {
                payload.remove("id");
                result = apiService.post("/api/timesheets", payload, token);
            }

            if (apiService.isSuccess(result)) {
                redirectAttributes.addFlashAttribute("success", "Timesheet saved successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", apiService.getErrorMessage(result));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save timesheet: " + e.getMessage());
        }
        return "redirect:/timesheets";
    }

    @PostMapping("/{id}/submit")
    public String submitTimesheet(@PathVariable Long id,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.post("/api/timesheets/" + id + "/submit", null, token);
            if (apiService.isSuccess(result)) {
                redirectAttributes.addFlashAttribute("success", "Timesheet submitted for approval");
            } else {
                redirectAttributes.addFlashAttribute("error", apiService.getErrorMessage(result));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to submit timesheet");
        }
        return "redirect:/timesheets/" + id;
    }

    @PostMapping("/{id}/approve")
    public String approveTimesheet(@PathVariable Long id,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        Map<String, Object> payload = new HashMap<>();
        payload.put("approved", true);

        try {
            Map<String, Object> result = apiService.post("/api/timesheets/" + id + "/review", payload, token);
            if (apiService.isSuccess(result)) {
                redirectAttributes.addFlashAttribute("success", "Timesheet approved");
            } else {
                redirectAttributes.addFlashAttribute("error", apiService.getErrorMessage(result));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to approve timesheet");
        }
        return "redirect:/timesheets/" + id;
    }

    @PostMapping("/{id}/reject")
    public String rejectTimesheet(@PathVariable Long id,
                                  @RequestParam String rejectionReason,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        Map<String, Object> payload = new HashMap<>();
        payload.put("approved", false);
        payload.put("rejectionReason", rejectionReason);

        try {
            Map<String, Object> result = apiService.post("/api/timesheets/" + id + "/review", payload, token);
            if (apiService.isSuccess(result)) {
                redirectAttributes.addFlashAttribute("success", "Timesheet rejected");
            } else {
                redirectAttributes.addFlashAttribute("error", apiService.getErrorMessage(result));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reject timesheet");
        }
        return "redirect:/timesheets/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteTimesheet(@PathVariable Long id,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        try {
            apiService.delete("/api/timesheets/" + id, token);
            redirectAttributes.addFlashAttribute("success", "Timesheet deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete timesheet");
        }
        return "redirect:/timesheets";
    }

    /**
     * Load the employee's active allocations and enrich with project names.
     * This replaces the old loadProjectsForEntries() — now employees only see
     * projects they are allocated to, along with hoursPerDay limits.
     */
    @SuppressWarnings("unchecked")
    private void loadAllocationsForEmployee(Long employeeId, String token, Model model) {
        List<Map<String, Object>> allocations = new ArrayList<>();
        try {
            Map<String, Object> result = apiService.get("/api/allocations/employee/" + employeeId, token);
            if (apiService.isSuccess(result)) {
                Object data = apiService.extractData(result);
                if (data instanceof List) {
                    allocations = (List<Map<String, Object>>) data;

                    // Enrich allocations with project names
                    Map<String, String> projectNameMap = buildProjectNameMap(token);
                    for (Map<String, Object> alloc : allocations) {
                        Object projId = alloc.get("projectId");
                        if (projId != null) {
                            String projectName = projectNameMap.getOrDefault(projId.toString(), "Project #" + projId);
                            alloc.put("projectName", projectName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore — allocations will be empty
        }
        model.addAttribute("allocations", allocations);
    }

    /**
     * Build a projectId → projectName lookup map
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> buildProjectNameMap(String token) {
        Map<String, String> map = new HashMap<>();
        try {
            Map<String, Object> result = apiService.get("/api/projects?page=0&size=100", token);
            if (apiService.isSuccess(result)) {
                Map<String, Object> data = (Map<String, Object>) apiService.extractData(result);
                if (data != null && data.get("content") != null) {
                    List<Map<String, Object>> projects = (List<Map<String, Object>>) data.get("content");
                    for (Map<String, Object> p : projects) {
                        String id = p.get("id") != null ? p.get("id").toString() : null;
                        String name = (String) p.get("name");
                        if (name == null) name = (String) p.get("projectName");
                        if (id != null && name != null) {
                            map.put(id, name);
                        }
                    }
                }
            }
        } catch (Exception e) { /* ignore */ }
        return map;
    }

    /**
     * Enrich timesheet entries with project names for the view page
     */
    @SuppressWarnings("unchecked")
    private void enrichTimesheetEntriesWithProjectNames(Map<String, Object> timesheet, String token) {
        try {
            Object entriesObj = timesheet.get("entries");
            if (entriesObj instanceof List) {
                List<Map<String, Object>> entries = (List<Map<String, Object>>) entriesObj;
                if (!entries.isEmpty()) {
                    Map<String, String> projectNameMap = buildProjectNameMap(token);
                    for (Map<String, Object> entry : entries) {
                        Object projId = entry.get("projectId");
                        if (projId != null) {
                            entry.put("projectName", projectNameMap.getOrDefault(projId.toString(), "Project #" + projId));
                        }
                    }
                }
            }
        } catch (Exception e) { /* ignore */ }
    }

    /**
     * Enrich timesheets list with employee names for admin/PMO views
     */
    @SuppressWarnings("unchecked")
    private void enrichTimesheetsWithEmployeeNames(List<Map<String, Object>> timesheets, String token) {
        Map<String, String> empNameMap = new HashMap<>();
        try {
            Map<String, Object> result = apiService.get("/api/auth/users/names", token);
            if (apiService.isSuccess(result)) {
                Map<String, Object> namesMap = (Map<String, Object>) apiService.extractData(result);
                if (namesMap != null) {
                    for (Map.Entry<String, Object> entry : namesMap.entrySet()) {
                        empNameMap.put(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                }
            }
        } catch (Exception e) { /* ignore */ }

        for (Map<String, Object> ts : timesheets) {
            Object empId = ts.get("employeeId");
            if (empId != null) {
                ts.put("employeeName", empNameMap.getOrDefault(empId.toString(), "Employee #" + empId));
            }
        }
    }

    private Map<String, Object> buildTimesheetPayload(Map<String, String> formData, Long employeeId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("weekStartDate", formData.get("weekStartDate"));
        payload.put("employeeId", employeeId);
        if (formData.get("id") != null && !formData.get("id").isEmpty()) {
            payload.put("id", formData.get("id"));
        }

        // Build entries from the form — use correct backend field names
        List<Map<String, Object>> entries = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            String projectId = formData.get("entries[" + i + "].projectId");
            String hours = formData.get("entries[" + i + "].hours");
            String entryDate = formData.get("entries[" + i + "].entryDate");
            String taskDescription = formData.get("entries[" + i + "].taskDescription");
            String allocationId = formData.get("entries[" + i + "].allocationId");

            if (projectId != null && !projectId.isEmpty() && hours != null && !hours.isEmpty()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("projectId", Long.parseLong(projectId));
                entry.put("hours", Double.parseDouble(hours));
                entry.put("entryDate", entryDate);
                entry.put("taskDescription", taskDescription);
                if (allocationId != null && !allocationId.isEmpty()) {
                    entry.put("allocationId", Long.parseLong(allocationId));
                }
                entries.add(entry);
            }
        }
        payload.put("entries", entries);
        return payload;
    }
}
