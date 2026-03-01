package com.eretain.timesheet.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimesheetApprovalDTO {

    @NotNull(message = "Approved status is required (true=approve, false=reject)")
    private Boolean approved;

    private String rejectionReason;
}
