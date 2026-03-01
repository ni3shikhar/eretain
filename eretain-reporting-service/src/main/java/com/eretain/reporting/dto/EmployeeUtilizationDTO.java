package com.eretain.reporting.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeUtilizationDTO {
    private Long employeeId;
    private String employeeName;
    private String designation;
    private Double totalAvailableHours;
    private Double totalAllocatedHours;
    private Double totalLoggedHours;
    private Double allocationUtilization; // allocatedHours / availableHours * 100
    private Double timesheetUtilization;  // loggedHours / availableHours * 100
    private Integer activeProjects;
    private List<AllocationReportDTO> allocations;
}
