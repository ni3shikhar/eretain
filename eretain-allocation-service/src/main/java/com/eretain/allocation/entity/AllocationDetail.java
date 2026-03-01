package com.eretain.allocation.entity;

import com.eretain.common.config.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "allocation_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationDetail extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id", nullable = false)
    private Allocation allocation;

    @Column(name = "allocation_date", nullable = false)
    private LocalDate allocationDate;

    @Column(name = "hours", nullable = false)
    private Double hours;

    @Column(name = "notes")
    private String notes;
}
