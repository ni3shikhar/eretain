package com.eretain.project.entity;

import com.eretain.common.config.AuditEntity;
import com.eretain.common.enums.BillabilityCategory;
import com.eretain.common.enums.ProjectStatus;
import com.eretain.common.enums.ProjectType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_code", nullable = false, unique = true)
    private String projectCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false)
    private ProjectType projectType;

    @Enumerated(EnumType.STRING)
    @Column(name = "billability_category", nullable = false)
    private BillabilityCategory billabilityCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProjectStatus status = ProjectStatus.PROPOSED;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "delivery_unit_id")
    private Long deliveryUnitId;

    @Column(name = "project_manager_id")
    private Long projectManagerId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "budget", precision = 15, scale = 2)
    private BigDecimal budget;

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProjectSchedule> schedules = new ArrayList<>();
}
