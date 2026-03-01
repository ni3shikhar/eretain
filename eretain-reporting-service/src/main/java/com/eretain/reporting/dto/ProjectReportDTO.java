package com.eretain.reporting.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectReportDTO {
    private Long projectId;
    private String projectName;
    private String projectCode;
    private String status;
    private String billabilityCategory;
    private String clientName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalAllocations;
    private Double totalAllocatedHours;
    private Double totalTimesheetHours;
    private Double budgetUtilization;
    private Double budget;
    private String currency;
}
