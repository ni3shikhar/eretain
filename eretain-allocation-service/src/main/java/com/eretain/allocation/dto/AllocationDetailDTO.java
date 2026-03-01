package com.eretain.allocation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationDetailDTO {
    private Long id;

    @NotNull(message = "Allocation ID is required")
    private Long allocationId;

    @NotNull(message = "Allocation date is required")
    private LocalDate allocationDate;

    @NotNull(message = "Hours is required")
    @Min(value = 0, message = "Minimum 0 hours")
    @Max(value = 8, message = "Maximum 8 hours per day")
    private Double hours;

    private String notes;
    private boolean active;
}
