package com.eretain.ui.controller;

import com.eretain.ui.service.ApiService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/allocations")
public class AllocationController {

    private final ApiService apiService;

    public AllocationController(ApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping
    public String listAllocations(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  Authentication authentication,
                                  HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        boolean isAdminOrPmo = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))
                || authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PMO"));

        try {
            String url;
            if (isAdminOrPmo) {
                url = "/api/allocations?page=" + page + "&size=" + size;
            } else {
                url = "/api/allocations/my-allocations";
            }
            Map<String, Object> result = apiService.get(url, token);
            if (apiService.isSuccess(result)) {
                Object data = apiService.extractData(result);
                List<Map<String, Object>> allocations;
                if (data instanceof Map) {
                    Map<String, Object> pagedData = (Map<String, Object>) data;
                    allocations = (List<Map<String, Object>>) pagedData.get("content");
                    model.addAttribute("currentPage", page);
                    model.addAttribute("totalPages", pagedData.get("totalPages"));
                    model.addAttribute("totalElements", pagedData.get("totalElements"));
                } else {
                    allocations = (List<Map<String, Object>>) data;
                }
                enrichAllocationsWithNames(allocations, token);
                model.addAttribute("allocations", allocations);
            }
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load allocations");
        }
        model.addAttribute("isAdminOrPmo", isAdminOrPmo);
        return "allocations/list";
    }

    @GetMapping("/new")
    public String newAllocation(HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        model.addAttribute("allocation", new HashMap<String, Object>());
        model.addAttribute("isEdit", false);
        loadSelectData(token, model);
        return "allocations/form";
    }

    @GetMapping("/{id}")
    @SuppressWarnings("unchecked")
    public String viewAllocation(@PathVariable Long id, HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/allocations/" + id, token);
            if (apiService.isSuccess(result)) {
                Map<String, Object> allocation = (Map<String, Object>) apiService.extractData(result);
                enrichAllocationWithNames(allocation, token);
                model.addAttribute("allocation", allocation);
                return "allocations/view";
            }
        } catch (Exception e) {
            // fall through
        }
        return "redirect:/allocations";
    }

    @GetMapping("/{id}/edit")
    public String editAllocation(@PathVariable Long id, HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/allocations/" + id, token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("allocation", apiService.extractData(result));
                model.addAttribute("isEdit", true);
                loadSelectData(token, model);
                return "allocations/form";
            }
        } catch (Exception e) {
            // fall through
        }
        return "redirect:/allocations";
    }

    @PostMapping
    public String saveAllocation(@RequestParam Map<String, String> formData,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        Map<String, Object> payload = new HashMap<>(formData);

        // Remap form field 'role' to backend field 'roleName'
        if (payload.containsKey("role")) {
            payload.put("roleName", payload.remove("role"));
        }
        // Remove fields not in AllocationDTO
        payload.remove("billabilityCategory");

        try {
            String id = formData.get("id");
            Map<String, Object> result;
            if (id != null && !id.isEmpty()) {
                result = apiService.put("/api/allocations/" + id, payload, token);
            } else {
                payload.remove("id");
                result = apiService.post("/api/allocations", payload, token);
            }

            if (apiService.isSuccess(result)) {
                redirectAttributes.addFlashAttribute("success", "Allocation saved successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", apiService.getErrorMessage(result));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save allocation");
        }
        return "redirect:/allocations";
    }

    @PostMapping("/{id}/delete")
    public String deleteAllocation(@PathVariable Long id,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        try {
            apiService.delete("/api/allocations/" + id, token);
            redirectAttributes.addFlashAttribute("success", "Allocation deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete allocation");
        }
        return "redirect:/allocations";
    }

    @SuppressWarnings("unchecked")
    private void loadSelectData(String token, Model model) {
        // Initialize with empty lists to prevent null in template
        model.addAttribute("projects", java.util.Collections.emptyList());
        model.addAttribute("employees", java.util.Collections.emptyList());

        // Load projects for dropdown
        try {
            Map<String, Object> result = apiService.get("/api/projects?page=0&size=100", token);
            if (apiService.isSuccess(result)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) apiService.extractData(result);
                if (data != null) {
                    model.addAttribute("projects", data.get("content"));
                }
            }
        } catch (Exception e) { /* ignore */ }

        // Load users for dropdown
        try {
            Map<String, Object> result = apiService.get("/api/auth/users?page=0&size=200", token);
            if (apiService.isSuccess(result)) {
                Object data = apiService.extractData(result);
                if (data instanceof Map) {
                    model.addAttribute("employees", ((Map<String, Object>) data).get("content"));
                } else {
                    model.addAttribute("employees", data);
                }
            }
        } catch (Exception e) { /* ignore */ }

        model.addAttribute("allocationStatuses", new String[]{"PROPOSED", "ACTIVE", "ON_HOLD", "COMPLETED", "CANCELLED"});

        // Load role names from rate cards for dropdown
        model.addAttribute("roles", java.util.Collections.emptyList());
        try {
            Map<String, Object> result = apiService.get("/api/auth/rate-cards/roles", token);
            if (apiService.isSuccess(result)) {
                Object data = apiService.extractData(result);
                if (data instanceof List) {
                    model.addAttribute("roles", data);
                }
            }
        } catch (Exception e) { /* ignore */ }
    }

    @SuppressWarnings("unchecked")
    private void enrichAllocationsWithNames(List<Map<String, Object>> allocations, String token) {
        if (allocations == null || allocations.isEmpty()) return;

        // Build employee ID -> name map using lightweight names endpoint
        Map<String, String> employeeNames = new HashMap<>();
        try {
            Map<String, Object> result = apiService.get("/api/auth/users/names", token);
            if (apiService.isSuccess(result)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> namesMap = (Map<String, Object>) apiService.extractData(result);
                if (namesMap != null) {
                    for (Map.Entry<String, Object> entry : namesMap.entrySet()) {
                        employeeNames.put(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                }
            }
        } catch (Exception e) { /* ignore */ }

        // Build project ID -> name map
        Map<String, String> projectNames = new HashMap<>();
        try {
            Map<String, Object> result = apiService.get("/api/projects?page=0&size=100", token);
            if (apiService.isSuccess(result)) {
                Map<String, Object> data = (Map<String, Object>) apiService.extractData(result);
                if (data != null) {
                    List<Map<String, Object>> projects = (List<Map<String, Object>>) data.get("content");
                    if (projects != null) {
                        for (Map<String, Object> p : projects) {
                            projectNames.put(String.valueOf(p.get("id")), String.valueOf(p.get("name")));
                        }
                    }
                }
            }
        } catch (Exception e) { /* ignore */ }

        // Enrich each allocation
        for (Map<String, Object> a : allocations) {
            String empId = String.valueOf(a.get("employeeId"));
            String projId = String.valueOf(a.get("projectId"));
            a.put("employeeName", employeeNames.getOrDefault(empId, empId));
            a.put("projectName", projectNames.getOrDefault(projId, projId));
        }
    }

    @SuppressWarnings("unchecked")
    private void enrichAllocationWithNames(Map<String, Object> allocation, String token) {
        if (allocation == null) return;
        enrichAllocationsWithNames(java.util.Collections.singletonList(allocation), token);
    }
}
