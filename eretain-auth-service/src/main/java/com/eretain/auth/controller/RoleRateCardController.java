package com.eretain.auth.controller;

import com.eretain.auth.dto.RoleRateCardDTO;
import com.eretain.auth.service.RoleRateCardService;
import com.eretain.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/rate-cards")
@RequiredArgsConstructor
public class RoleRateCardController {

    private final RoleRateCardService rateCardService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<List<RoleRateCardDTO>>> getAllRateCards() {
        return ResponseEntity.ok(ApiResponse.success(rateCardService.getAllRateCards()));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PMO')")
    public ResponseEntity<ApiResponse<List<String>>> getActiveRoleNames() {
        return ResponseEntity.ok(ApiResponse.success(rateCardService.getActiveRoleNames()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<RoleRateCardDTO>> getRateCardById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(rateCardService.getRateCardById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<RoleRateCardDTO>> createRateCard(@Valid @RequestBody RoleRateCardDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Rate card created", rateCardService.createRateCard(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<RoleRateCardDTO>> updateRateCard(@PathVariable Long id,
                                                                        @Valid @RequestBody RoleRateCardDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Rate card updated", rateCardService.updateRateCard(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<Void>> deleteRateCard(@PathVariable Long id) {
        rateCardService.deleteRateCard(id);
        return ResponseEntity.ok(ApiResponse.success("Rate card deleted", null));
    }
}
