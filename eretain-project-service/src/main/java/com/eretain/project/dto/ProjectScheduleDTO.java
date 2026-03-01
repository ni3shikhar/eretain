package com.eretain.project.dto;

import jakarta.validation.constraints.NotBlank;
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
public class ProjectScheduleDTO {
    private Long id;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotBlank(message = "Phase name is required")
    private String phaseName;

    private String description;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Double plannedHours;
    private Integer sortOrder;
    private boolean active;
}
