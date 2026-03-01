package com.eretain.auth.entity;

import com.eretain.common.config.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_access")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccess extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "module_name", nullable = false)
    private String moduleName; // ALLOCATION, TIMESHEET, PROJECT, COMPANY, REPORTING

    @Column(name = "can_read")
    private boolean canRead = false;

    @Column(name = "can_write")
    private boolean canWrite = false;

    @Column(name = "can_delete")
    private boolean canDelete = false;

    @Column(name = "is_enabled")
    private boolean enabled = true;
}
