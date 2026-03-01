package com.eretain.ui.controller;

import com.eretain.ui.service.ApiService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ApiService apiService;

    public ReportController(ApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping
    public String reportsHome(Authentication authentication, Model model) {
        boolean isAdminOrPmo = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))
                || authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PMO"));
        model.addAttribute("isAdminOrPmo", isAdminOrPmo);
        return "reports/index";
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/projects")
    public String projectReport(@RequestParam(required = false) String status,
                                HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            String url = "/api/reports/projects";
            if (status != null && !status.isEmpty()) {
                url += "?status=" + status;
            }
            Map<String, Object> result = apiService.get(url, token);
            if (apiService.isSuccess(result)) {
                Object data = apiService.extractData(result);
                model.addAttribute("reportData", data);

                // Compute summary counts for chart
                if (data instanceof java.util.List) {
                    java.util.List<Map<String, Object>> projects = (java.util.List<Map<String, Object>>) data;
                    Map<String, Long> statusCounts = new java.util.LinkedHashMap<>();
                    statusCounts.put("ACTIVE", 0L);
                    statusCounts.put("PROPOSED", 0L);
                    statusCounts.put("ON_HOLD", 0L);
                    statusCounts.put("COMPLETED", 0L);
                    statusCounts.put("CANCELLED", 0L);
                    for (Map<String, Object> p : projects) {
                        String s = String.valueOf(p.getOrDefault("status", "UNKNOWN"));
                        statusCounts.merge(s, 1L, Long::sum);
                    }
                    model.addAttribute("statusCounts", statusCounts);

                    Map<String, Object> summary = new java.util.HashMap<>();
                    summary.put("totalProjects", projects.size());
                    summary.put("activeProjects", statusCounts.getOrDefault("ACTIVE", 0L));
                    summary.put("completedProjects", statusCounts.getOrDefault("COMPLETED", 0L));
                    summary.put("onHoldProjects", statusCounts.getOrDefault("ON_HOLD", 0L));
                    model.addAttribute("summary", summary);
                }
            }
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load project report");
        }
        model.addAttribute("selectedStatus", status);
        return "reports/projects";
    }

    @GetMapping("/allocations")
    public String allocationReport(@RequestParam(required = false) Long projectId,
                                   HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            String url = "/api/reports/allocations";
            if (projectId != null) {
                url += "?projectId=" + projectId;
            }
            Map<String, Object> result = apiService.get(url, token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("reportData", apiService.extractData(result));
            }
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load allocation report");
        }
        return "reports/allocations";
    }

    @GetMapping("/timesheets")
    public String timesheetReport(@RequestParam(required = false) String startDate,
                                  @RequestParam(required = false) String endDate,
                                  HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            String url = "/api/reports/timesheets";
            String separator = "?";
            if (startDate != null && !startDate.isEmpty()) {
                url += separator + "startDate=" + startDate;
                separator = "&";
            }
            if (endDate != null && !endDate.isEmpty()) {
                url += separator + "endDate=" + endDate;
            }
            Map<String, Object> result = apiService.get(url, token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("reportData", apiService.extractData(result));
            }
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load timesheet report");
        }
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "reports/timesheets";
    }

    @GetMapping("/utilization")
    public String utilizationReport(Authentication authentication,
                                    @RequestParam(required = false) String level,
                                    @RequestParam(required = false) Long id,
                                    @RequestParam(required = false) String startDate,
                                    @RequestParam(required = false) String endDate,
                                    @RequestParam(required = false) String breadcrumb,
                                    HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        boolean isAdminOrPmo = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))
                || authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PMO"));

        try {
            if (isAdminOrPmo) {
                String dateParams = buildDateParams(startDate, endDate);

                if (level != null && id != null) {
                    // Drill-down into specific level
                    String url = "/api/reports/utilization/level/" + level + "/" + id + dateParams;
                    Map<String, Object> result = apiService.get(url, token);
                    if (apiService.isSuccess(result)) {
                        Object data = apiService.extractData(result);
                        model.addAttribute("currentNode", data);
                        if (data instanceof Map) {
                            model.addAttribute("children", ((Map<String, Object>) data).get("children"));
                        }
                    }
                    model.addAttribute("currentLevel", level);
                    model.addAttribute("currentId", id);
                    model.addAttribute("drillDown", true);
                } else {
                    // Top-level overview: show all Business Units
                    String url = "/api/reports/utilization/overview" + dateParams;
                    Map<String, Object> result = apiService.get(url, token);
                    if (apiService.isSuccess(result)) {
                        model.addAttribute("children", apiService.extractData(result));
                    }
                    model.addAttribute("currentLevel", "OVERVIEW");
                    model.addAttribute("drillDown", false);
                }
                model.addAttribute("hierarchyView", true);
            } else {
                // Employee: show own utilization
                String url = "/api/reports/my-utilization";
                Map<String, Object> result = apiService.get(url, token);
                if (apiService.isSuccess(result)) {
                    Object data = apiService.extractData(result);
                    model.addAttribute("utilization", data);
                    model.addAttribute("summary", data);
                }
                model.addAttribute("hierarchyView", false);
            }
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load utilization report: " + e.getMessage());
        }
        model.addAttribute("isAdminOrPmo", isAdminOrPmo);
        model.addAttribute("breadcrumb", breadcrumb != null ? breadcrumb : "");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "reports/utilization";
    }

    private String buildDateParams(String startDate, String endDate) {
        StringBuilder sb = new StringBuilder();
        String sep = "?";
        if (startDate != null && !startDate.isEmpty()) {
            sb.append(sep).append("startDate=").append(startDate);
            sep = "&";
        }
        if (endDate != null && !endDate.isEmpty()) {
            sb.append(sep).append("endDate=").append(endDate);
        }
        return sb.toString();
    }

    @GetMapping("/my-allocations")
    public String myAllocations(HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/reports/my-allocations", token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("reportData", apiService.extractData(result));
            }
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load your allocations");
        }
        return "reports/my-allocations";
    }

    @GetMapping("/my-timesheets")
    public String myTimesheets(HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/reports/my-timesheets", token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("reportData", apiService.extractData(result));
            }
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load your timesheets report");
        }
        return "reports/my-timesheets";
    }
}
