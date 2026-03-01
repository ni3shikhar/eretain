package com.eretain.company.repository;

import com.eretain.company.entity.DeliveryUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryUnitRepository extends JpaRepository<DeliveryUnit, Long> {
    List<DeliveryUnit> findByUnitId(Long unitId);
    List<DeliveryUnit> findByUnitIdAndActiveTrue(Long unitId);
    boolean existsByCodeAndUnitId(String code, Long unitId);
    List<DeliveryUnit> findByActiveTrue();
    Optional<DeliveryUnit> findByIdAndActiveTrue(Long id);
}
