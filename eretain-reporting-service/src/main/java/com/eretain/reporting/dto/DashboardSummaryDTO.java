package com.eretain.reporting.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDTO {
    private Long totalEmployees;
    private Long activeProjects;
    private Long totalAllocations;
    private Long pendingTimesheets;
    private Double averageUtilization;
    private Map<String, Long> projectsByStatus;
    private Map<String, Long> allocationsByStatus;
    private Map<String, Double> timesheetHoursByWeek;
}
