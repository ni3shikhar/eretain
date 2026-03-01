package com.eretain.timesheet.service;

import com.eretain.common.dto.PagedResponse;
import com.eretain.common.enums.TimesheetStatus;
import com.eretain.common.exception.BusinessValidationException;
import com.eretain.common.exception.ResourceNotFoundException;
import com.eretain.timesheet.dto.TimesheetApprovalDTO;
import com.eretain.timesheet.dto.TimesheetDTO;
import com.eretain.timesheet.dto.TimesheetEntryDTO;
import com.eretain.timesheet.entity.Timesheet;
import com.eretain.timesheet.entity.TimesheetEntry;
import com.eretain.timesheet.repository.TimesheetEntryRepository;
import com.eretain.timesheet.repository.TimesheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimesheetService {

    private static final double MAX_HOURS_PER_DAY = 8.0;
    private static final double MAX_HOURS_PER_WEEK = 40.0;

    private final TimesheetRepository timesheetRepository;
    private final TimesheetEntryRepository entryRepository;

    // =================== Timesheet Operations ===================

    public PagedResponse<TimesheetDTO> getAllTimesheets(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("weekStartDate").descending());
        Page<Timesheet> timesheets = timesheetRepository.findByActiveTrue(pageable);
        return mapToPagedResponse(timesheets);
    }

    public PagedResponse<TimesheetDTO> getTimesheetsByEmployee(Long employeeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("weekStartDate").descending());
        Page<Timesheet> timesheets = timesheetRepository.findByEmployeeIdAndActiveTrue(employeeId, pageable);
        return mapToPagedResponse(timesheets);
    }

    public TimesheetDTO getTimesheetById(Long id) {
        return mapToDTO(timesheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet", "id", id)));
    }

    public PagedResponse<TimesheetDTO> getTimesheetsByStatus(TimesheetStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("weekStartDate").descending());
        Page<Timesheet> timesheets = timesheetRepository.findByStatus(status, pageable);
        return mapToPagedResponse(timesheets);
    }

    @Transactional
    public TimesheetDTO createTimesheet(TimesheetDTO dto) {
        // Calculate week boundaries (Monday to Friday)
        LocalDate weekStart = dto.getWeekStartDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(4); // Friday

        // Check for existing timesheet for the same week
        timesheetRepository.findByEmployeeIdAndWeekStartDate(dto.getEmployeeId(), weekStart)
                .ifPresent(existing -> {
                    throw new BusinessValidationException(
                            String.format("Timesheet already exists for employee %d for week starting %s",
                                    dto.getEmployeeId(), weekStart));
                });

        Timesheet timesheet = Timesheet.builder()
                .employeeId(dto.getEmployeeId())
                .weekStartDate(weekStart)
                .weekEndDate(weekEnd)
                .status(TimesheetStatus.DRAFT)
                .totalHours(0.0)
                .notes(dto.getNotes())
                .build();

        Timesheet savedTimesheet = timesheetRepository.save(timesheet);

        // If entries are provided in the payload, create them
        double totalHours = 0.0;
        if (dto.getEntries() != null && !dto.getEntries().isEmpty()) {
            for (TimesheetEntryDTO entryDto : dto.getEntries()) {
                TimesheetEntry entry = TimesheetEntry.builder()
                        .timesheet(savedTimesheet)
                        .projectId(entryDto.getProjectId())
                        .allocationId(entryDto.getAllocationId())
                        .entryDate(entryDto.getEntryDate())
                        .hours(entryDto.getHours())
                        .taskDescription(entryDto.getTaskDescription())
                        .billable(entryDto.getBillable() != null ? entryDto.getBillable() : true)
                        .notes(entryDto.getNotes())
                        .build();
                entryRepository.save(entry);
                totalHours += entryDto.getHours();
            }
            savedTimesheet.setTotalHours(totalHours);
            savedTimesheet = timesheetRepository.save(savedTimesheet);
        }

        return mapToDTO(savedTimesheet);
    }

    @Transactional
    public TimesheetDTO submitTimesheet(Long id) {
        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet", "id", id));

        if (timesheet.getStatus() != TimesheetStatus.DRAFT && timesheet.getStatus() != TimesheetStatus.REJECTED) {
            throw new BusinessValidationException(
                    "Only DRAFT or REJECTED timesheets can be submitted. Current status: " + timesheet.getStatus());
        }

        // Recalculate total hours
        Double totalHours = entryRepository.getTotalHoursForTimesheet(id);
        if (totalHours <= 0) {
            throw new BusinessValidationException("Cannot submit a timesheet with no entries");
        }

        timesheet.setTotalHours(totalHours);
        timesheet.setStatus(TimesheetStatus.SUBMITTED);
        timesheet.setSubmittedDate(LocalDate.now());

        return mapToDTO(timesheetRepository.save(timesheet));
    }

    @Transactional
    public TimesheetDTO approveOrRejectTimesheet(Long id, TimesheetApprovalDTO approvalDTO, Long approverId) {
        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet", "id", id));

        if (timesheet.getStatus() != TimesheetStatus.SUBMITTED) {
            throw new BusinessValidationException(
                    "Only SUBMITTED timesheets can be approved/rejected. Current status: " + timesheet.getStatus());
        }

        if (approvalDTO.getApproved()) {
            timesheet.setStatus(TimesheetStatus.APPROVED);
            timesheet.setApprovedBy(approverId);
            timesheet.setApprovedDate(LocalDate.now());
            timesheet.setRejectionReason(null);
        } else {
            if (approvalDTO.getRejectionReason() == null || approvalDTO.getRejectionReason().isBlank()) {
                throw new BusinessValidationException("Rejection reason is required when rejecting a timesheet");
            }
            timesheet.setStatus(TimesheetStatus.REJECTED);
            timesheet.setRejectionReason(approvalDTO.getRejectionReason());
        }

        return mapToDTO(timesheetRepository.save(timesheet));
    }

    @Transactional
    public void deleteTimesheet(Long id) {
        Timesheet timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet", "id", id));

        if (timesheet.getStatus() == TimesheetStatus.APPROVED) {
            throw new BusinessValidationException("Cannot delete an approved timesheet");
        }

        timesheet.setActive(false);
        timesheetRepository.save(timesheet);
    }

    // =================== Timesheet Entry Operations ===================

    @Transactional
    public TimesheetEntryDTO createEntry(TimesheetEntryDTO dto) {
        Timesheet timesheet = timesheetRepository.findById(dto.getTimesheetId())
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet", "id", dto.getTimesheetId()));

        if (timesheet.getStatus() != TimesheetStatus.DRAFT && timesheet.getStatus() != TimesheetStatus.REJECTED) {
            throw new BusinessValidationException(
                    "Can only add entries to DRAFT or REJECTED timesheets. Current status: " + timesheet.getStatus());
        }

        // Validate date is within timesheet week
        if (dto.getEntryDate().isBefore(timesheet.getWeekStartDate()) ||
                dto.getEntryDate().isAfter(timesheet.getWeekEndDate())) {
            throw new BusinessValidationException(
                    String.format("Entry date %s must be within timesheet week %s to %s",
                            dto.getEntryDate(), timesheet.getWeekStartDate(), timesheet.getWeekEndDate()));
        }

        // Validate daily limit: 8 hours/day
        Double existingDailyHours = entryRepository.getTotalHoursForEmployeeOnDate(
                timesheet.getEmployeeId(), dto.getEntryDate());
        if (existingDailyHours + dto.getHours() > MAX_HOURS_PER_DAY) {
            throw new BusinessValidationException(
                    String.format("Cannot log %.1f hours on %s. Employee already has %.1f hours logged. " +
                            "Maximum is %.1f hours per day.", dto.getHours(), dto.getEntryDate(),
                            existingDailyHours, MAX_HOURS_PER_DAY));
        }

        // Validate weekly limit: 40 hours/week
        Double existingWeeklyHours = entryRepository.getTotalHoursForTimesheet(timesheet.getId());
        if (existingWeeklyHours + dto.getHours() > MAX_HOURS_PER_WEEK) {
            throw new BusinessValidationException(
                    String.format("Cannot log %.1f hours. Timesheet already has %.1f hours. " +
                            "Maximum is %.1f hours per week.", dto.getHours(), existingWeeklyHours, MAX_HOURS_PER_WEEK));
        }

        TimesheetEntry entry = TimesheetEntry.builder()
                .timesheet(timesheet)
                .projectId(dto.getProjectId())
                .allocationId(dto.getAllocationId())
                .entryDate(dto.getEntryDate())
                .hours(dto.getHours())
                .taskDescription(dto.getTaskDescription())
                .billable(dto.getBillable() != null ? dto.getBillable() : true)
                .notes(dto.getNotes())
                .build();

        TimesheetEntry saved = entryRepository.save(entry);

        // Update total hours on timesheet
        timesheet.setTotalHours(entryRepository.getTotalHoursForTimesheet(timesheet.getId()));
        timesheetRepository.save(timesheet);

        return mapEntryToDTO(saved);
    }

    @Transactional
    public TimesheetEntryDTO updateEntry(Long id, TimesheetEntryDTO dto) {
        TimesheetEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimesheetEntry", "id", id));

        Timesheet timesheet = entry.getTimesheet();
        if (timesheet.getStatus() != TimesheetStatus.DRAFT && timesheet.getStatus() != TimesheetStatus.REJECTED) {
            throw new BusinessValidationException(
                    "Can only edit entries on DRAFT or REJECTED timesheets. Current status: " + timesheet.getStatus());
        }

        if (dto.getHours() != null) {
            // Validate daily limit excluding current entry
            Double existingDailyHours = entryRepository.getTotalHoursForEmployeeOnDate(
                    timesheet.getEmployeeId(), entry.getEntryDate());
            double netDaily = existingDailyHours - entry.getHours() + dto.getHours();
            if (netDaily > MAX_HOURS_PER_DAY) {
                throw new BusinessValidationException(
                        String.format("Update would exceed %.1f hours per day limit. Total would be %.1f hours.",
                                MAX_HOURS_PER_DAY, netDaily));
            }

            entry.setHours(dto.getHours());
        }

        if (dto.getProjectId() != null) entry.setProjectId(dto.getProjectId());
        if (dto.getAllocationId() != null) entry.setAllocationId(dto.getAllocationId());
        if (dto.getTaskDescription() != null) entry.setTaskDescription(dto.getTaskDescription());
        if (dto.getBillable() != null) entry.setBillable(dto.getBillable());
        if (dto.getNotes() != null) entry.setNotes(dto.getNotes());

        TimesheetEntry saved = entryRepository.save(entry);

        // Update total hours on timesheet
        timesheet.setTotalHours(entryRepository.getTotalHoursForTimesheet(timesheet.getId()));
        timesheetRepository.save(timesheet);

        return mapEntryToDTO(saved);
    }

    @Transactional
    public void deleteEntry(Long id) {
        TimesheetEntry entry = entryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimesheetEntry", "id", id));

        Timesheet timesheet = entry.getTimesheet();
        if (timesheet.getStatus() != TimesheetStatus.DRAFT && timesheet.getStatus() != TimesheetStatus.REJECTED) {
            throw new BusinessValidationException(
                    "Can only delete entries on DRAFT or REJECTED timesheets. Current status: " + timesheet.getStatus());
        }

        entry.setActive(false);
        entryRepository.save(entry);

        timesheet.setTotalHours(entryRepository.getTotalHoursForTimesheet(timesheet.getId()));
        timesheetRepository.save(timesheet);
    }

    public List<TimesheetEntryDTO> getEntriesByTimesheet(Long timesheetId) {
        return entryRepository.findByTimesheetIdAndActiveTrue(timesheetId).stream()
                .map(this::mapEntryToDTO)
                .collect(Collectors.toList());
    }

    // =================== Mappers ===================

    private TimesheetDTO mapToDTO(Timesheet entity) {
        List<TimesheetEntryDTO> entries = entity.getEntries() != null
                ? entity.getEntries().stream()
                    .filter(e -> e.isActive())
                    .map(this::mapEntryToDTO)
                    .collect(Collectors.toList())
                : null;

        return TimesheetDTO.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .weekStartDate(entity.getWeekStartDate())
                .weekEndDate(entity.getWeekEndDate())
                .status(entity.getStatus())
                .totalHours(entity.getTotalHours())
                .submittedDate(entity.getSubmittedDate())
                .approvedBy(entity.getApprovedBy())
                .approvedDate(entity.getApprovedDate())
                .rejectionReason(entity.getRejectionReason())
                .notes(entity.getNotes())
                .active(entity.isActive())
                .entries(entries)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private TimesheetEntryDTO mapEntryToDTO(TimesheetEntry entity) {
        return TimesheetEntryDTO.builder()
                .id(entity.getId())
                .timesheetId(entity.getTimesheet().getId())
                .projectId(entity.getProjectId())
                .allocationId(entity.getAllocationId())
                .entryDate(entity.getEntryDate())
                .hours(entity.getHours())
                .taskDescription(entity.getTaskDescription())
                .billable(entity.getBillable())
                .notes(entity.getNotes())
                .active(entity.isActive())
                .build();
    }

    private PagedResponse<TimesheetDTO> mapToPagedResponse(Page<Timesheet> page) {
        return PagedResponse.<TimesheetDTO>builder()
                .content(page.getContent().stream().map(this::mapToDTO).collect(Collectors.toList()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
