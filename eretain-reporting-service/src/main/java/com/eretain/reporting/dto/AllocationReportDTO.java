package com.eretain.reporting.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationReportDTO {
    private Long employeeId;
    private String employeeName;
    private Long projectId;
    private String projectName;
    private String roleName;
    private String allocationStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double hoursPerDay;
    private Double totalAllocatedHours;
    private Double utilizationPercentage;
}
