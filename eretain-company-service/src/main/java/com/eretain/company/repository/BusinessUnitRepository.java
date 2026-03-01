package com.eretain.company.repository;

import com.eretain.company.entity.BusinessUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessUnitRepository extends JpaRepository<BusinessUnit, Long> {
    Optional<BusinessUnit> findByCode(String code);
    boolean existsByCode(String code);
    List<BusinessUnit> findByActiveTrue();
    Optional<BusinessUnit> findByIdAndActiveTrue(Long id);
}
