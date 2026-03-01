package com.eretain.ui.controller;

import com.eretain.ui.service.ApiService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    private final ApiService apiService;

    public DashboardController(ApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Authentication authentication, HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);

        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"));
        boolean isPmo = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PMO"));

        if (isAdmin || isPmo) {
            loadAdminDashboard(token, model);
        } else {
            loadEmployeeDashboard(token, model, authentication.getName());
        }

        return "dashboard";
    }

    @SuppressWarnings("unchecked")
    private void loadAdminDashboard(String token, Model model) {
        try {
            // Load projects
            Map<String, Object> projectsResult = apiService.get("/api/projects?page=0&size=5", token);
            if (apiService.isSuccess(projectsResult)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) apiService.extractData(projectsResult);
                if (data != null) {
                    model.addAttribute("recentProjects", data.get("content"));
                    model.addAttribute("totalProjects", data.get("totalElements"));
                }
            }
        } catch (Exception e) {
            // Silently handle - dashboard should not break
        }

        try {
            // Load business units count
            Map<String, Object> buResult = apiService.get("/api/company/business-units", token);
            if (apiService.isSuccess(buResult)) {
                Object data = apiService.extractData(buResult);
                if (data instanceof List) {
                    model.addAttribute("totalBusinessUnits", ((List<?>) data).size());
                }
            }
        } catch (Exception e) {
            // Silently handle
        }

        try {
            // Load timesheets
            Map<String, Object> tsResult = apiService.get("/api/timesheets?page=0&size=5", token);
            if (apiService.isSuccess(tsResult)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) apiService.extractData(tsResult);
                if (data != null) {
                    model.addAttribute("recentTimesheets", data.get("content"));
                    model.addAttribute("pendingTimesheets", 0);
                }
            }
        } catch (Exception e) {
            // Silently handle
        }
    }

    @SuppressWarnings("unchecked")
    private void loadEmployeeDashboard(String token, Model model, String username) {
        try {
            Map<String, Object> allocResult = apiService.get("/api/allocations/my-allocations", token);
            if (apiService.isSuccess(allocResult)) {
                Object data = apiService.extractData(allocResult);
                if (data instanceof List) {
                    model.addAttribute("myAllocations", ((List<?>) data).size());
                }
            }
        } catch (Exception e) {
            // Silently handle
        }

        try {
            Map<String, Object> tsResult = apiService.get("/api/timesheets/my-timesheets?page=0&size=5", token);
            if (apiService.isSuccess(tsResult)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) apiService.extractData(tsResult);
                if (data != null) {
                    model.addAttribute("recentTimesheets", data.get("content"));
                }
            }
        } catch (Exception e) {
            // Silently handle
        }
    }
}
