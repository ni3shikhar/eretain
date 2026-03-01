package com.eretain.allocation.dto;

import com.eretain.common.enums.AllocationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationDTO {
    private Long id;

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private Long projectScheduleId;
    private String roleName;
    private AllocationStatus status;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Hours per day is required")
    @Min(value = 1, message = "Minimum 1 hour per day")
    @Max(value = 8, message = "Maximum 8 hours per day")
    private Double hoursPerDay;

    private String notes;
    private boolean active;
    private List<AllocationDetailDTO> details;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
