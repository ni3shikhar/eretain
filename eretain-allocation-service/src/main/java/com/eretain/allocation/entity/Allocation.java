package com.eretain.allocation.entity;

import com.eretain.common.config.AuditEntity;
import com.eretain.common.enums.AllocationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Allocation extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "project_schedule_id")
    private Long projectScheduleId;

    @Column(name = "role_name")
    private String roleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AllocationStatus status = AllocationStatus.PROPOSED;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "hours_per_day", nullable = false)
    private Double hoursPerDay;

    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "allocation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AllocationDetail> details = new ArrayList<>();
}
