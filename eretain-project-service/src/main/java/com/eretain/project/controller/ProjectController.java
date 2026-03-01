package com.eretain.project.controller;

import com.eretain.common.dto.ApiResponse;
import com.eretain.common.dto.PagedResponse;
import com.eretain.project.dto.BulkUploadResultDTO;
import com.eretain.project.dto.ProjectDTO;
import com.eretain.project.dto.ProjectScheduleDTO;
import com.eretain.project.service.BulkUploadService;
import com.eretain.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final BulkUploadService bulkUploadService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<PagedResponse<ProjectDTO>>> getAllProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getAllProjects(page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ProjectDTO>> getProject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProjectById(id)));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ProjectDTO>> getProjectByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProjectByCode(code)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<ProjectDTO>>> getActiveProjects() {
        return ResponseEntity.ok(ApiResponse.success(projectService.getActiveProjects()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<ProjectDTO>> createProject(@Valid @RequestBody ProjectDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Project created", projectService.createProject(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<ProjectDTO>> updateProject(@PathVariable Long id,
                                                                   @Valid @RequestBody ProjectDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Project updated", projectService.updateProject(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.success("Project deleted", null));
    }

    // =================== Bulk Upload Endpoints ===================

    @GetMapping("/bulk-upload/template")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<byte[]> downloadTemplate() {
        String csvContent = bulkUploadService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=project_upload_template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent.getBytes());
    }

    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<BulkUploadResultDTO>> bulkUpload(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Please select a CSV file to upload"));
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Only CSV files are supported"));
        }
        BulkUploadResultDTO result = bulkUploadService.processUpload(file);
        String message = "Bulk upload completed: " + result.getSuccessCount() + " succeeded, " +
                result.getFailureCount() + " failed out of " + result.getTotalRecords() + " records";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    // =================== Schedule Endpoints ===================

    @GetMapping("/{projectId}/schedules")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<ProjectScheduleDTO>>> getSchedules(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProjectSchedules(projectId)));
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<ProjectScheduleDTO>> createSchedule(
            @Valid @RequestBody ProjectScheduleDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Schedule created", projectService.createSchedule(dto)));
    }

    @PutMapping("/schedules/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<ProjectScheduleDTO>> updateSchedule(
            @PathVariable Long id, @Valid @RequestBody ProjectScheduleDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Schedule updated", projectService.updateSchedule(id, dto)));
    }

    @DeleteMapping("/schedules/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(@PathVariable Long id) {
        projectService.deleteSchedule(id);
        return ResponseEntity.ok(ApiResponse.success("Schedule deleted", null));
    }
}
