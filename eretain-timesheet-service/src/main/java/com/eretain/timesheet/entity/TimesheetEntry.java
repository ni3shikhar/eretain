package com.eretain.timesheet.entity;

import com.eretain.common.config.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "timesheet_entries", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"timesheet_id", "project_id", "entry_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimesheetEntry extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_id", nullable = false)
    private Timesheet timesheet;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "allocation_id")
    private Long allocationId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false)
    private Double hours;

    @Column(name = "task_description", length = 500)
    private String taskDescription;

    @Column(name = "is_billable")
    @Builder.Default
    private Boolean billable = true;

    @Column(length = 500)
    private String notes;
}
