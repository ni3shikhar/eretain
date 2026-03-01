package com.eretain.company.repository;

import com.eretain.company.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {
    List<Unit> findByBusinessUnitId(Long businessUnitId);
    List<Unit> findByBusinessUnitIdAndActiveTrue(Long businessUnitId);
    boolean existsByCodeAndBusinessUnitId(String code, Long businessUnitId);
    List<Unit> findByActiveTrue();
    Optional<Unit> findByIdAndActiveTrue(Long id);
}
