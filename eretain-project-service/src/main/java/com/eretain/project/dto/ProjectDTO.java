package com.eretain.project.dto;

import com.eretain.common.enums.BillabilityCategory;
import com.eretain.common.enums.ProjectStatus;
import com.eretain.common.enums.ProjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {
    private Long id;

    @NotBlank(message = "Project code is required")
    private String projectCode;

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;

    @NotNull(message = "Project type is required")
    private ProjectType projectType;

    @NotNull(message = "Billability category is required")
    private BillabilityCategory billabilityCategory;

    private ProjectStatus status;
    private String clientName;
    private Long deliveryUnitId;
    private Long projectManagerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private String currency;
    private boolean active;
    private List<ProjectScheduleDTO> schedules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
