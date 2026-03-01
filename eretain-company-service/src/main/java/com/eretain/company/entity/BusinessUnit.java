package com.eretain.company.entity;

import com.eretain.common.config.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "business_units")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessUnit extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "businessUnit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Unit> units = new ArrayList<>();
}
