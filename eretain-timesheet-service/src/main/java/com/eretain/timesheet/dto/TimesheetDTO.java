package com.eretain.timesheet.dto;

import com.eretain.common.enums.TimesheetStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimesheetDTO {

    private Long id;

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Week start date is required")
    private LocalDate weekStartDate;

    private LocalDate weekEndDate;

    private TimesheetStatus status;

    private Double totalHours;

    private LocalDate submittedDate;

    private Long approvedBy;

    private LocalDate approvedDate;

    private String rejectionReason;

    private String notes;

    private boolean active;

    private List<TimesheetEntryDTO> entries;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
