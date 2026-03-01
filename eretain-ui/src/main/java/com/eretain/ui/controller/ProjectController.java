package com.eretain.ui.controller;

import com.eretain.ui.service.ApiService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/projects")
public class ProjectController {

    private final ApiService apiService;

    public ProjectController(ApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping
    public String listProjects(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/projects?page=" + page + "&size=" + size, token);
            if (apiService.isSuccess(result)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) apiService.extractData(result);
                if (data != null) {
                    model.addAttribute("projects", data.get("content"));
                    model.addAttribute("currentPage", page);
                    model.addAttribute("totalPages", data.get("totalPages"));
                    model.addAttribute("totalElements", data.get("totalElements"));
                }
            }
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load projects");
        }
        return "projects/list";
    }

    @GetMapping("/{id}")
    public String viewProject(@PathVariable Long id, HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/projects/" + id, token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("project", apiService.extractData(result));
                // Load schedules
                try {
                    Map<String, Object> schedResult = apiService.get("/api/projects/" + id + "/schedules", token);
                    if (apiService.isSuccess(schedResult)) {
                        model.addAttribute("schedules", apiService.extractData(schedResult));
                    }
                } catch (Exception e) { /* ignore */ }
                return "projects/view";
            }
        } catch (Exception e) {
            // fall through
        }
        return "redirect:/projects";
    }

    @GetMapping("/new")
    public String newProject(HttpServletRequest request, Model model) {
        model.addAttribute("project", new HashMap<String, Object>());
        model.addAttribute("isEdit", false);
        addProjectTypes(model);
        addProjectStatuses(model);
        addDeliveryUnits(model, request);
        return "projects/form";
    }

    @GetMapping("/{id}/edit")
    public String editProject(@PathVariable Long id, HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        try {
            Map<String, Object> result = apiService.get("/api/projects/" + id, token);
            if (apiService.isSuccess(result)) {
                model.addAttribute("project", apiService.extractData(result));
                model.addAttribute("isEdit", true);
                addProjectTypes(model);
                addProjectStatuses(model);
                addDeliveryUnits(model, request);
                return "projects/form";
            }
        } catch (Exception e) {
            // fall through
        }
        return "redirect:/projects";
    }

    @PostMapping
    public String saveProject(@RequestParam Map<String, String> formData,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        Map<String, Object> payload = new HashMap<>(formData);

        // Remap form field names to match backend DTO
        if (payload.containsKey("projectName")) {
            payload.put("name", payload.remove("projectName"));
        }

        try {
            String id = formData.get("id");
            Map<String, Object> result;
            if (id != null && !id.isEmpty()) {
                result = apiService.put("/api/projects/" + id, payload, token);
            } else {
                payload.remove("id");
                result = apiService.post("/api/projects", payload, token);
            }

            if (apiService.isSuccess(result)) {
                redirectAttributes.addFlashAttribute("success", "Project saved successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", apiService.getErrorMessage(result));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save project");
        }
        return "redirect:/projects";
    }

    @PostMapping("/{id}/delete")
    public String deleteProject(@PathVariable Long id,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        try {
            apiService.delete("/api/projects/" + id, token);
            redirectAttributes.addFlashAttribute("success", "Project deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete project");
        }
        return "redirect:/projects";
    }

    // ======== Project Schedules ========

    @PostMapping("/{projectId}/schedules")
    public String saveSchedule(@PathVariable Long projectId,
                               @RequestParam Map<String, String> formData,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        String token = apiService.extractToken(request);
        Map<String, Object> payload = new HashMap<>(formData);
        payload.put("projectId", projectId);

        try {
            String id = formData.get("id");
            Map<String, Object> result;
            if (id != null && !id.isEmpty()) {
                result = apiService.put("/api/projects/" + projectId + "/schedules/" + id, payload, token);
            } else {
                payload.remove("id");
                result = apiService.post("/api/projects/" + projectId + "/schedules", payload, token);
            }

            if (apiService.isSuccess(result)) {
                redirectAttributes.addFlashAttribute("success", "Schedule saved successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", apiService.getErrorMessage(result));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save schedule");
        }
        return "redirect:/projects/" + projectId;
    }

    // ======== Bulk Upload ========

    @GetMapping("/bulk-upload")
    public String bulkUploadForm(Model model) {
        model.addAttribute("activeMenu", "projects");
        return "projects/bulk-upload";
    }

    @PostMapping("/bulk-upload")
    public String processBulkUpload(@RequestParam("file") MultipartFile file,
                                    HttpServletRequest request, Model model) {
        String token = apiService.extractToken(request);
        model.addAttribute("activeMenu", "projects");

        if (file.isEmpty()) {
            model.addAttribute("error", "Please select a CSV file to upload");
            return "projects/bulk-upload";
        }

        try {
            Map<String, Object> result = apiService.uploadFile("/api/projects/bulk-upload", file, token);
            if (apiService.isSuccess(result)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) apiService.extractData(result);
                model.addAttribute("uploadResult", data);
                model.addAttribute("success", result.get("message"));
            } else {
                model.addAttribute("error", apiService.getErrorMessage(result));
            }
        } catch (Exception e) {
            model.addAttribute("error", "Failed to process bulk upload: " + e.getMessage());
        }

        return "projects/bulk-upload";
    }

    @GetMapping("/bulk-upload/template")
    public ResponseEntity<byte[]> downloadTemplate(HttpServletRequest request) {
        String token = apiService.extractToken(request);
        try {
            byte[] content = apiService.getBytes("/api/projects/bulk-upload/template", token);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=project_upload_template.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private void addProjectTypes(Model model) {
        model.addAttribute("projectTypes", new String[]{"FIXED_PRICE", "TIME_AND_MATERIAL", "INTERNAL", "SUPPORT"});
    }

    private void addProjectStatuses(Model model) {
        model.addAttribute("projectStatuses", new String[]{"PROPOSED", "ACTIVE", "ON_HOLD", "COMPLETED", "CANCELLED"});
    }

    @SuppressWarnings("unchecked")
    private void addDeliveryUnits(Model model, HttpServletRequest request) {
        String token = apiService.extractToken(request);
        List<Map<String, Object>> deliveryUnits = new ArrayList<>();
        try {
            Map<String, Object> result = apiService.get("/api/company/delivery-units", token);
            if (apiService.isSuccess(result)) {
                Object data = apiService.extractData(result);
                if (data instanceof List) {
                    deliveryUnits = (List<Map<String, Object>>) data;
                }
            }
        } catch (Exception e) {
            // If company service is unavailable, dropdown will be empty
        }
        model.addAttribute("deliveryUnits", deliveryUnits);
    }
}
