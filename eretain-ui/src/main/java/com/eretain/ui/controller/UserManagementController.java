package com.eretain.ui.controller;

import com.eretain.ui.service.ApiService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/admin/users")
public class UserManagementController {

    private final ApiService apiService;

    public UserManagementController(ApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping
    @SuppressWarnings("unchecked")
    public String listUsers(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/auth/users?page=" + page + "&size=" + size, token);
            if (apiService.isSuccess(result)) {
                Object data = apiService.extractData(result);
                if (data instanceof Map) {
                    Map<String, Object> pagedData = (Map<String, Object>) data;
                    model.addAttribute("users", pagedData.get("content"));
                    model.addAttribute("currentPage", page);
                    model.addAttribute("totalPages", pagedData.get("totalPages"));
                    model.addAttribute("totalElements", pagedData.get("totalElements"));
                }
            }
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load users");
        }
        model.addAttribute("activeMenu", "users");
        return "admin/users/list";
    }

    @GetMapping("/{id}")
    public String viewUser(@PathVariable Long id, HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/auth/users/" + id, token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("user", apiService.extractData(result));
                // Load user access
                try {
                    Map<String, Object> accessResult = apiService.get("/api/auth/users/" + id + "/access", token);
                    if (apiService.isSuccess(accessResult)) {
                        model.addAttribute("accesses", apiService.extractData(accessResult));
                    }
                } catch (Exception e) { /* ignore */ }
                model.addAttribute("activeMenu", "users");
                return "admin/users/view";
            }
        } catch (Exception e) {
            // fall through
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/new")
    public String newUser(Model model) {
        model.addAttribute("user", new HashMap<String, Object>());
        model.addAttribute("isEdit", false);
        addRoles(model);
        model.addAttribute("activeMenu", "users");
        return "admin/users/form";
    }

    @GetMapping("/{id}/edit")
    public String editUser(@PathVariable Long id, HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/auth/users/" + id, token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("user", apiService.extractData(result));
                model.addAttribute("isEdit", true);
                addRoles(model);
                model.addAttribute("activeMenu", "users");
                return "admin/users/form";
            }
        } catch (Exception e) {
            // fall through
        }
        return "redirect:/admin/users";
    }

    @PostMapping
    public String saveUser(@RequestParam Map<String, String> formData,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        Map<String, Object> payload = new HashMap<>(formData);

        // Convert roles from comma-separated to Set
        String rolesStr = formData.get("roles");
        if (rolesStr != null && !rolesStr.isEmpty()) {
            payload.put("roles", new HashSet<>(Arrays.asList(rolesStr.split(","))));
        }

        try {
            String id = formData.get("id");
            Map<String, Object> result;
            if (id != null && !id.isEmpty()) {
                payload.remove("id");
                // For update, remove password if empty
                if (payload.get("password") == null || payload.get("password").toString().isEmpty()) {
                    payload.remove("password");
                }
                result = apiService.put("/api/auth/users/" + id, payload, token);
            } else {
                payload.remove("id");
                result = apiService.post("/api/auth/users", payload, token);
            }

            if (apiService.isSuccess(result)) {
                redirectAttributes.addFlashAttribute("success", "User saved successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", apiService.getErrorMessage(result));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        try {
            apiService.delete("/api/auth/users/" + id, token);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete user");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/enable")
    public String enableUser(@PathVariable Long id,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        try {
            // Use PATCH via a workaround — send as PUT with enable flag
            Map<String, Object> patchResult = apiService.put("/api/auth/users/" + id + "/enable", Map.of(), token);
            redirectAttributes.addFlashAttribute("success", "User enabled");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to enable user");
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/disable")
    public String disableUser(@PathVariable Long id,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> patchResult = apiService.put("/api/auth/users/" + id + "/disable", Map.of(), token);
            redirectAttributes.addFlashAttribute("success", "User disabled");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to disable user");
        }
        return "redirect:/admin/users/" + id;
    }

    private void addRoles(Model model) {
        model.addAttribute("availableRoles", new String[]{"ADMINISTRATOR", "PMO", "EMPLOYEE"});
    }
}
