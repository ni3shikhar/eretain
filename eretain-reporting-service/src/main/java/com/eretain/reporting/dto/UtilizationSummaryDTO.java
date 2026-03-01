package com.eretain.reporting.dto;

import lombok.*;

import java.util.List;

/**
 * Hierarchical utilization summary that works at every level:
 * Business Unit → Unit → Delivery Unit → Project → Employee
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilizationSummaryDTO {

    private Long id;
    private String name;
    private String code;

    /** BUSINESS_UNIT, UNIT, DELIVERY_UNIT, PROJECT, EMPLOYEE */
    private String level;

    private Integer headCount;
    private Double totalAvailableHours;
    private Double totalAllocatedHours;
    private Double utilizationPercentage;

    private Integer activeProjects;
    private Integer totalAllocations;

    /** Child summaries for drill-down */
    private List<UtilizationSummaryDTO> children;
}
