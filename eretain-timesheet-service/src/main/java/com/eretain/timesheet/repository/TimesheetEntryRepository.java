package com.eretain.timesheet.repository;

import com.eretain.timesheet.entity.TimesheetEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimesheetEntryRepository extends JpaRepository<TimesheetEntry, Long> {

    List<TimesheetEntry> findByTimesheetIdAndActiveTrue(Long timesheetId);

    List<TimesheetEntry> findByTimesheetIdAndProjectIdAndActiveTrue(Long timesheetId, Long projectId);

    @Query("SELECT COALESCE(SUM(te.hours), 0) FROM TimesheetEntry te " +
            "WHERE te.timesheet.id = :timesheetId AND te.entryDate = :date AND te.active = true")
    Double getTotalHoursForDate(@Param("timesheetId") Long timesheetId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(te.hours), 0) FROM TimesheetEntry te " +
            "WHERE te.timesheet.id = :timesheetId AND te.active = true")
    Double getTotalHoursForTimesheet(@Param("timesheetId") Long timesheetId);

    @Query("SELECT COALESCE(SUM(te.hours), 0) FROM TimesheetEntry te " +
            "WHERE te.timesheet.employeeId = :employeeId AND te.entryDate = :date " +
            "AND te.active = true")
    Double getTotalHoursForEmployeeOnDate(
            @Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(te.hours), 0) FROM TimesheetEntry te " +
            "WHERE te.timesheet.employeeId = :employeeId " +
            "AND te.projectId = :projectId " +
            "AND te.entryDate BETWEEN :startDate AND :endDate AND te.active = true")
    Double getTotalHoursForEmployeeOnProject(
            @Param("employeeId") Long employeeId,
            @Param("projectId") Long projectId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
