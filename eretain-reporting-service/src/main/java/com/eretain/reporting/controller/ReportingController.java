package com.eretain.reporting.controller;

import com.eretain.common.dto.ApiResponse;
import com.eretain.reporting.dto.*;
import com.eretain.reporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    /**
     * Project reports - available to Administrator and PMO
     */
    @GetMapping("/projects")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<List<ProjectReportDTO>>> getProjectReport(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long deliveryUnitId) {

        ReportFilterDTO filter = ReportFilterDTO.builder()
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .deliveryUnitId(deliveryUnitId)
                .build();

        return ResponseEntity.ok(ApiResponse.success(reportingService.getProjectReport(filter)));
    }

    /**
     * Allocation reports - Administrator/PMO see all, Employee sees own
     */
    @GetMapping("/allocations")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<List<AllocationReportDTO>>> getAllocationReport(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        ReportFilterDTO filter = ReportFilterDTO.builder()
                .employeeId(employeeId)
                .projectId(projectId)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        return ResponseEntity.ok(ApiResponse.success(reportingService.getAllocationReport(filter)));
    }

    /**
     * Employee's own allocation report
     */
    @GetMapping("/my-allocations")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<AllocationReportDTO>>> getMyAllocationReport(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        ReportFilterDTO filter = ReportFilterDTO.builder()
                .employeeId(userId)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        return ResponseEntity.ok(ApiResponse.success(reportingService.getAllocationReport(filter)));
    }

    /**
     * Timesheet reports - Administrator/PMO see all, Employee sees own
     */
    @GetMapping("/timesheets")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<List<TimesheetReportDTO>>> getTimesheetReport(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        ReportFilterDTO filter = ReportFilterDTO.builder()
                .employeeId(employeeId)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        return ResponseEntity.ok(ApiResponse.success(reportingService.getTimesheetReport(filter)));
    }

    /**
     * Employee's own timesheet report
     */
    @GetMapping("/my-timesheets")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<TimesheetReportDTO>>> getMyTimesheetReport(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        ReportFilterDTO filter = ReportFilterDTO.builder()
                .employeeId(userId)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        return ResponseEntity.ok(ApiResponse.success(reportingService.getTimesheetReport(filter)));
    }

    /**
     * Hierarchical utilization overview - shows all Business Units with drill-down
     */
    @GetMapping("/utilization/overview")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<List<UtilizationSummaryDTO>>> getUtilizationOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null) {
            startDate = LocalDate.now().withMonth(1).withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now().withMonth(12).withDayOfMonth(31);
        }
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getUtilizationOverview(startDate, endDate)));
    }

    /**
     * Drill-down utilization by level (BUSINESS_UNIT, UNIT, DELIVERY_UNIT, PROJECT, EMPLOYEE)
     */
    @GetMapping("/utilization/level/{level}/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<UtilizationSummaryDTO>> getUtilizationByLevel(
            @PathVariable String level,
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null) {
            startDate = LocalDate.now().withMonth(1).withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now().withMonth(12).withDayOfMonth(31);
        }
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getUtilizationByLevel(level.toUpperCase(), id, startDate, endDate)));
    }

    /**
     * Employee utilization report
     */
    @GetMapping("/utilization/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<EmployeeUtilizationDTO>> getEmployeeUtilization(
            @PathVariable Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null) {
            startDate = LocalDate.now().withMonth(1).withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now().withMonth(12).withDayOfMonth(31);
        }
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getEmployeeUtilization(employeeId, startDate, endDate)));
    }

    /**
     * Employee's own utilization report
     */
    @GetMapping("/my-utilization")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeUtilizationDTO>> getMyUtilization(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null) {
            startDate = LocalDate.now().withMonth(1).withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now().withMonth(12).withDayOfMonth(31);
        }
        return ResponseEntity.ok(ApiResponse.success(
                reportingService.getEmployeeUtilization(userId, startDate, endDate)));
    }
}
