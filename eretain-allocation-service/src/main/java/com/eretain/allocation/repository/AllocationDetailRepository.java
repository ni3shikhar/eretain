package com.eretain.allocation.repository;

import com.eretain.allocation.entity.AllocationDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AllocationDetailRepository extends JpaRepository<AllocationDetail, Long> {

    List<AllocationDetail> findByAllocationId(Long allocationId);
    List<AllocationDetail> findByAllocationIdAndActiveTrue(Long allocationId);

    @Query("SELECT COALESCE(SUM(ad.hours), 0) FROM AllocationDetail ad " +
           "JOIN ad.allocation a " +
           "WHERE a.employeeId = :employeeId " +
           "AND ad.allocationDate = :date " +
           "AND ad.active = true")
    Double getTotalHoursForEmployeeOnDate(
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(ad.hours), 0) FROM AllocationDetail ad " +
           "JOIN ad.allocation a " +
           "WHERE a.employeeId = :employeeId " +
           "AND ad.allocationDate BETWEEN :weekStart AND :weekEnd " +
           "AND ad.active = true")
    Double getTotalWeeklyHoursForEmployee(
            @Param("employeeId") Long employeeId,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd);
}
