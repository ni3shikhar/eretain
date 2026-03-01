package com.eretain.company.controller;

import com.eretain.company.dto.*;
import com.eretain.company.service.CompanyService;
import com.eretain.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    // =================== Business Units ===================

    @GetMapping("/business-units")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<List<BusinessUnitDTO>>> getAllBusinessUnits() {
        return ResponseEntity.ok(ApiResponse.success(companyService.getAllBusinessUnits()));
    }

    @GetMapping("/business-units/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<BusinessUnitDTO>> getBusinessUnit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(companyService.getBusinessUnitById(id)));
    }

    @PostMapping("/business-units")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<BusinessUnitDTO>> createBusinessUnit(
            @Valid @RequestBody BusinessUnitDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Business unit created", companyService.createBusinessUnit(dto)));
    }

    @PutMapping("/business-units/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<BusinessUnitDTO>> updateBusinessUnit(
            @PathVariable Long id, @Valid @RequestBody BusinessUnitDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Business unit updated", companyService.updateBusinessUnit(id, dto)));
    }

    @DeleteMapping("/business-units/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<Void>> deleteBusinessUnit(@PathVariable Long id) {
        companyService.deleteBusinessUnit(id);
        return ResponseEntity.ok(ApiResponse.success("Business unit deleted", null));
    }

    // =================== Units ===================

    @GetMapping("/units")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<List<UnitDTO>>> getAllUnits() {
        return ResponseEntity.ok(ApiResponse.success(companyService.getAllUnits()));
    }

    @GetMapping("/business-units/{businessUnitId}/units")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<List<UnitDTO>>> getUnits(@PathVariable Long businessUnitId) {
        return ResponseEntity.ok(ApiResponse.success(companyService.getUnitsByBusinessUnit(businessUnitId)));
    }

    @GetMapping("/units/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<UnitDTO>> getUnit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(companyService.getUnitById(id)));
    }

    @PostMapping("/units")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<UnitDTO>> createUnit(@Valid @RequestBody UnitDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Unit created", companyService.createUnit(dto)));
    }

    @PutMapping("/units/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<UnitDTO>> updateUnit(@PathVariable Long id,
                                                            @Valid @RequestBody UnitDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Unit updated", companyService.updateUnit(id, dto)));
    }

    @DeleteMapping("/units/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<Void>> deleteUnit(@PathVariable Long id) {
        companyService.deleteUnit(id);
        return ResponseEntity.ok(ApiResponse.success("Unit deleted", null));
    }

    // =================== Delivery Units ===================

    @GetMapping("/delivery-units")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<List<DeliveryUnitDTO>>> getAllDeliveryUnits() {
        return ResponseEntity.ok(ApiResponse.success(companyService.getAllDeliveryUnits()));
    }

    @GetMapping("/units/{unitId}/delivery-units")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<List<DeliveryUnitDTO>>> getDeliveryUnits(@PathVariable Long unitId) {
        return ResponseEntity.ok(ApiResponse.success(companyService.getDeliveryUnitsByUnit(unitId)));
    }

    @GetMapping("/delivery-units/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<DeliveryUnitDTO>> getDeliveryUnit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(companyService.getDeliveryUnitById(id)));
    }

    @PostMapping("/delivery-units")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<DeliveryUnitDTO>> createDeliveryUnit(
            @Valid @RequestBody DeliveryUnitDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Delivery unit created", companyService.createDeliveryUnit(dto)));
    }

    @PutMapping("/delivery-units/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<DeliveryUnitDTO>> updateDeliveryUnit(
            @PathVariable Long id, @Valid @RequestBody DeliveryUnitDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Delivery unit updated", companyService.updateDeliveryUnit(id, dto)));
    }

    @DeleteMapping("/delivery-units/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<Void>> deleteDeliveryUnit(@PathVariable Long id) {
        companyService.deleteDeliveryUnit(id);
        return ResponseEntity.ok(ApiResponse.success("Delivery unit deleted", null));
    }
}
