package com.eretain.timesheet.controller;

import com.eretain.common.dto.ApiResponse;
import com.eretain.common.dto.PagedResponse;
import com.eretain.common.enums.TimesheetStatus;
import com.eretain.timesheet.dto.TimesheetApprovalDTO;
import com.eretain.timesheet.dto.TimesheetDTO;
import com.eretain.timesheet.dto.TimesheetEntryDTO;
import com.eretain.timesheet.service.TimesheetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetService timesheetService;

    // =================== Timesheet Operations ===================

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<PagedResponse<TimesheetDTO>>> getAllTimesheets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                timesheetService.getAllTimesheets(page, size)));
    }

    @GetMapping("/my-timesheets")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<PagedResponse<TimesheetDTO>>> getMyTimesheets(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long employeeId = Long.valueOf(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(ApiResponse.success(
                timesheetService.getTimesheetsByEmployee(employeeId, page, size)));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO') or @securityService.isOwner(#employeeId)")
    public ResponseEntity<ApiResponse<PagedResponse<TimesheetDTO>>> getTimesheetsByEmployee(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                timesheetService.getTimesheetsByEmployee(employeeId, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<TimesheetDTO>> getTimesheetById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(timesheetService.getTimesheetById(id)));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<PagedResponse<TimesheetDTO>>> getTimesheetsByStatus(
            @PathVariable TimesheetStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                timesheetService.getTimesheetsByStatus(status, page, size)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<TimesheetDTO>> createTimesheet(@Valid @RequestBody TimesheetDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(timesheetService.createTimesheet(dto)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<TimesheetDTO>> submitTimesheet(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(timesheetService.submitTimesheet(id)));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<TimesheetDTO>> approveOrRejectTimesheet(
            @PathVariable Long id,
            @Valid @RequestBody TimesheetApprovalDTO approvalDTO,
            HttpServletRequest request) {
        Long approverId = Long.valueOf(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(ApiResponse.success(
                timesheetService.approveOrRejectTimesheet(id, approvalDTO, approverId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<Void>> deleteTimesheet(@PathVariable Long id) {
        timesheetService.deleteTimesheet(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // =================== Timesheet Entry Operations ===================

    @GetMapping("/{timesheetId}/entries")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<TimesheetEntryDTO>>> getEntriesByTimesheet(
            @PathVariable Long timesheetId) {
        return ResponseEntity.ok(ApiResponse.success(timesheetService.getEntriesByTimesheet(timesheetId)));
    }

    @PostMapping("/entries")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<TimesheetEntryDTO>> createEntry(
            @Valid @RequestBody TimesheetEntryDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(timesheetService.createEntry(dto)));
    }

    @PutMapping("/entries/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<TimesheetEntryDTO>> updateEntry(
            @PathVariable Long id, @Valid @RequestBody TimesheetEntryDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(timesheetService.updateEntry(id, dto)));
    }

    @DeleteMapping("/entries/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> deleteEntry(@PathVariable Long id) {
        timesheetService.deleteEntry(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
