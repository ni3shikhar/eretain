package com.eretain.reporting.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportFilterDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long employeeId;
    private Long projectId;
    private Long deliveryUnitId;
    private String status;
}
