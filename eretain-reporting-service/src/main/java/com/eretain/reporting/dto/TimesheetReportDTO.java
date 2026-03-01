package com.eretain.reporting.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimesheetReportDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private String status;
    private Double totalHours;
    private Double billableHours;
    private Double nonBillableHours;
    private Integer totalProjects;
    private LocalDate submittedDate;
}
