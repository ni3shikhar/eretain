package com.eretain.allocation.repository;

import com.eretain.allocation.entity.Allocation;
import com.eretain.common.enums.AllocationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AllocationRepository extends JpaRepository<Allocation, Long> {

    List<Allocation> findByEmployeeId(Long employeeId);
    List<Allocation> findByEmployeeIdAndActiveTrue(Long employeeId);
    List<Allocation> findByProjectId(Long projectId);
    Page<Allocation> findByProjectId(Long projectId, Pageable pageable);
    Page<Allocation> findByActiveTrue(Pageable pageable);
    Page<Allocation> findByProjectIdAndActiveTrue(Long projectId, Pageable pageable);
    List<Allocation> findByEmployeeIdAndStatus(Long employeeId, AllocationStatus status);

    java.util.Optional<Allocation> findByIdAndActiveTrue(Long id);

    @Query("SELECT a FROM Allocation a WHERE a.employeeId = :employeeId " +
           "AND a.active = true " +
           "AND ((a.startDate <= :endDate AND a.endDate >= :startDate))")
    List<Allocation> findOverlappingAllocations(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(a.hoursPerDay), 0) FROM Allocation a " +
           "WHERE a.employeeId = :employeeId " +
           "AND a.active = true " +
           "AND a.status IN ('ACTIVE', 'PROPOSED') " +
           "AND a.startDate <= :date AND a.endDate >= :date")
    Double getTotalHoursForEmployeeOnDate(
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(a.hoursPerDay), 0) FROM Allocation a " +
           "WHERE a.employeeId = :employeeId " +
           "AND a.active = true " +
           "AND a.status IN ('ACTIVE', 'PROPOSED') " +
           "AND a.startDate <= :weekEnd AND a.endDate >= :weekStart " +
           "AND a.id != :excludeId")
    Double getTotalWeeklyHoursExcluding(
            @Param("employeeId") Long employeeId,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd,
            @Param("excludeId") Long excludeId);
}
