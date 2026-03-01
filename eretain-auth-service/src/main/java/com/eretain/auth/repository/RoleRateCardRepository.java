package com.eretain.auth.repository;

import com.eretain.auth.entity.RoleRateCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRateCardRepository extends JpaRepository<RoleRateCard, Long> {
    Optional<RoleRateCard> findByRoleName(String roleName);
    boolean existsByRoleName(String roleName);
    List<RoleRateCard> findByActiveTrue();

    @Query("SELECT r.roleName FROM RoleRateCard r WHERE r.active = true ORDER BY r.roleName")
    List<String> findActiveRoleNames();
}
