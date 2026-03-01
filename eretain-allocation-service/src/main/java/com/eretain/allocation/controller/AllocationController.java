package com.eretain.allocation.controller;

import com.eretain.allocation.dto.AllocationDTO;
import com.eretain.allocation.dto.AllocationDetailDTO;
import com.eretain.allocation.service.AllocationService;
import com.eretain.common.dto.ApiResponse;
import com.eretain.common.dto.PagedResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/allocations")
@RequiredArgsConstructor
public class AllocationController {

    private final AllocationService allocationService;

    // =================== Allocations ===================

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<PagedResponse<AllocationDTO>>> getAllAllocations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(allocationService.getAllAllocations(page, size)));
    }

    @GetMapping("/my-allocations")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<AllocationDTO>>> getMyAllocations(HttpServletRequest request) {
        Long employeeId = Long.valueOf(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(ApiResponse.success(allocationService.getAllocationsByEmployee(employeeId)));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO') or @securityService.isOwner(#employeeId)")
    public ResponseEntity<ApiResponse<List<AllocationDTO>>> getAllocationsByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success(allocationService.getAllocationsByEmployee(employeeId)));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<PagedResponse<AllocationDTO>>> getAllocationsByProject(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(allocationService.getAllocationsByProject(projectId, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<AllocationDTO>> getAllocationById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(allocationService.getAllocationById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<AllocationDTO>> createAllocation(@Valid @RequestBody AllocationDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(allocationService.createAllocation(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<AllocationDTO>> updateAllocation(
            @PathVariable Long id, @Valid @RequestBody AllocationDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(allocationService.updateAllocation(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<Void>> deleteAllocation(@PathVariable Long id) {
        allocationService.deleteAllocation(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // =================== Allocation Details ===================

    @GetMapping("/{allocationId}/details")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<AllocationDetailDTO>>> getDetailsByAllocation(
            @PathVariable Long allocationId) {
        return ResponseEntity.ok(ApiResponse.success(allocationService.getDetailsByAllocation(allocationId)));
    }

    @PostMapping("/details")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<AllocationDetailDTO>> createAllocationDetail(
            @Valid @RequestBody AllocationDetailDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(allocationService.createAllocationDetail(dto)));
    }

    @PutMapping("/details/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<AllocationDetailDTO>> updateAllocationDetail(
            @PathVariable Long id, @Valid @RequestBody AllocationDetailDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(allocationService.updateAllocationDetail(id, dto)));
    }

    @DeleteMapping("/details/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<Void>> deleteAllocationDetail(@PathVariable Long id) {
        allocationService.deleteAllocationDetail(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
