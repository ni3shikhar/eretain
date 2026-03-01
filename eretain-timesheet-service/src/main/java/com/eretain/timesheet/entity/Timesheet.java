package com.eretain.timesheet.entity;

import com.eretain.common.config.AuditEntity;
import com.eretain.common.enums.TimesheetStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "timesheets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"employee_id", "week_start_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Timesheet extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TimesheetStatus status = TimesheetStatus.DRAFT;

    @Column(name = "total_hours")
    @Builder.Default
    private Double totalHours = 0.0;

    @Column(name = "submitted_date")
    private LocalDate submittedDate;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_date")
    private LocalDate approvedDate;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "timesheet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TimesheetEntry> entries = new ArrayList<>();
}
