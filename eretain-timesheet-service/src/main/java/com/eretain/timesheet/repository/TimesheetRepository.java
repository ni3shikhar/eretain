package com.eretain.timesheet.repository;

import com.eretain.common.enums.TimesheetStatus;
import com.eretain.timesheet.entity.Timesheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {

    Page<Timesheet> findByActiveTrue(Pageable pageable);

    Page<Timesheet> findByEmployeeIdAndActiveTrue(Long employeeId, Pageable pageable);

    Optional<Timesheet> findByEmployeeIdAndWeekStartDate(Long employeeId, LocalDate weekStartDate);

    List<Timesheet> findByEmployeeIdAndStatusAndActiveTrue(Long employeeId, TimesheetStatus status);

    @Query("SELECT t FROM Timesheet t WHERE t.status = :status AND t.active = true")
    Page<Timesheet> findByStatus(@Param("status") TimesheetStatus status, Pageable pageable);

    @Query("SELECT t FROM Timesheet t WHERE t.employeeId = :employeeId " +
            "AND t.weekStartDate BETWEEN :startDate AND :endDate AND t.active = true")
    List<Timesheet> findByEmployeeIdAndDateRange(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.totalHours), 0) FROM Timesheet t " +
            "WHERE t.employeeId = :employeeId AND t.status = 'APPROVED' " +
            "AND t.weekStartDate BETWEEN :startDate AND :endDate AND t.active = true")
    Double getTotalApprovedHours(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
