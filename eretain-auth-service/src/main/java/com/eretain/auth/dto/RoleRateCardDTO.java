package com.eretain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleRateCardDTO {
    private Long id;

    @NotBlank(message = "Role name is required")
    private String roleName;

    @NotNull(message = "Audax rate is required")
    @Positive(message = "Audax rate must be positive")
    private BigDecimal audaxRate;

    @NotNull(message = "Fixed fee rate is required")
    @Positive(message = "Fixed fee rate must be positive")
    private BigDecimal fixedFeeRate;

    @NotNull(message = "T&M rate is required")
    @Positive(message = "T&M rate must be positive")
    private BigDecimal tmRate;

    private String currency;
    private String description;
    private boolean active;
}
