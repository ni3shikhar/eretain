package com.eretain.timesheet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimesheetEntryDTO {

    private Long id;

    @NotNull(message = "Timesheet ID is required")
    private Long timesheetId;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private Long allocationId;

    @NotNull(message = "Entry date is required")
    private LocalDate entryDate;

    @NotNull(message = "Hours are required")
    @Min(value = 0, message = "Hours must be at least 0")
    @Max(value = 8, message = "Hours per entry cannot exceed 8")
    private Double hours;

    private String taskDescription;

    private Boolean billable;

    private String notes;

    private boolean active;
}
