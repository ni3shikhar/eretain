package com.eretain.auth.entity;

import com.eretain.common.config.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "role_rate_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRateCard extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false, unique = true)
    private String roleName;

    @Column(name = "audax_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal audaxRate;

    @Column(name = "fixed_fee_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal fixedFeeRate;

    @Column(name = "tm_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal tmRate;

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Column(name = "description")
    private String description;
}
