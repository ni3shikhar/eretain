package com.eretain.allocation.service;

import com.eretain.allocation.dto.AllocationDTO;
import com.eretain.allocation.dto.AllocationDetailDTO;
import com.eretain.allocation.entity.Allocation;
import com.eretain.allocation.entity.AllocationDetail;
import com.eretain.allocation.repository.AllocationDetailRepository;
import com.eretain.allocation.repository.AllocationRepository;
import com.eretain.common.dto.PagedResponse;
import com.eretain.common.enums.AllocationStatus;
import com.eretain.common.exception.BusinessValidationException;
import com.eretain.common.exception.ResourceNotFoundException;
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
public class AllocationService {

    private static final double MAX_HOURS_PER_DAY = 8.0;
    private static final double MAX_HOURS_PER_WEEK = 40.0;

    private final AllocationRepository allocationRepository;
    private final AllocationDetailRepository detailRepository;

    public PagedResponse<AllocationDTO> getAllAllocations(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
        Page<Allocation> allocations = allocationRepository.findByActiveTrue(pageable);
        return mapToPagedResponse(allocations);
    }

    public List<AllocationDTO> getAllocationsByEmployee(Long employeeId) {
        return allocationRepository.findByEmployeeIdAndActiveTrue(employeeId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public PagedResponse<AllocationDTO> getAllocationsByProject(Long projectId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());
        Page<Allocation> allocations = allocationRepository.findByProjectIdAndActiveTrue(projectId, pageable);
        return mapToPagedResponse(allocations);
    }

    public AllocationDTO getAllocationById(Long id) {
        return mapToDTO(allocationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", "id", id)));
    }

    @Transactional
    public AllocationDTO createAllocation(AllocationDTO dto) {
        validateAllocationConstraints(dto, null);

        Allocation allocation = Allocation.builder()
                .employeeId(dto.getEmployeeId())
                .projectId(dto.getProjectId())
                .projectScheduleId(dto.getProjectScheduleId())
                .roleName(dto.getRoleName())
                .status(dto.getStatus() != null ? dto.getStatus() : AllocationStatus.PROPOSED)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .hoursPerDay(dto.getHoursPerDay())
                .notes(dto.getNotes())
                .build();

        return mapToDTO(allocationRepository.save(allocation));
    }

    @Transactional
    public AllocationDTO updateAllocation(Long id, AllocationDTO dto) {
        Allocation allocation = allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", "id", id));

        // Validate with exclusion of current allocation
        dto.setEmployeeId(allocation.getEmployeeId());
        validateAllocationConstraints(dto, id);

        if (dto.getProjectId() != null) allocation.setProjectId(dto.getProjectId());
        if (dto.getProjectScheduleId() != null) allocation.setProjectScheduleId(dto.getProjectScheduleId());
        if (dto.getRoleName() != null) allocation.setRoleName(dto.getRoleName());
        if (dto.getStatus() != null) allocation.setStatus(dto.getStatus());
        if (dto.getStartDate() != null) allocation.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) allocation.setEndDate(dto.getEndDate());
        if (dto.getHoursPerDay() != null) allocation.setHoursPerDay(dto.getHoursPerDay());
        if (dto.getNotes() != null) allocation.setNotes(dto.getNotes());

        return mapToDTO(allocationRepository.save(allocation));
    }

    @Transactional
    public void deleteAllocation(Long id) {
        Allocation allocation = allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", "id", id));
        allocation.setActive(false);
        allocation.setStatus(AllocationStatus.CANCELLED);
        allocationRepository.save(allocation);
    }

    // =================== Allocation Details ===================

    @Transactional
    public AllocationDetailDTO createAllocationDetail(AllocationDetailDTO dto) {
        Allocation allocation = allocationRepository.findById(dto.getAllocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", "id", dto.getAllocationId()));

        // Validate daily limit: 8 hours/day
        Double existingDailyHours = detailRepository.getTotalHoursForEmployeeOnDate(
                allocation.getEmployeeId(), dto.getAllocationDate());
        if (existingDailyHours + dto.getHours() > MAX_HOURS_PER_DAY) {
            throw new BusinessValidationException(
                    String.format("Cannot allocate %.1f hours on %s. Employee already has %.1f hours allocated. " +
                            "Maximum is %.1f hours per day.", dto.getHours(), dto.getAllocationDate(),
                            existingDailyHours, MAX_HOURS_PER_DAY));
        }

        // Validate weekly limit: 40 hours/week
        LocalDate weekStart = dto.getAllocationDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = dto.getAllocationDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        Double existingWeeklyHours = detailRepository.getTotalWeeklyHoursForEmployee(
                allocation.getEmployeeId(), weekStart, weekEnd);
        if (existingWeeklyHours + dto.getHours() > MAX_HOURS_PER_WEEK) {
            throw new BusinessValidationException(
                    String.format("Cannot allocate %.1f hours in week %s to %s. Employee already has %.1f hours " +
                            "allocated. Maximum is %.1f hours per week.", dto.getHours(), weekStart, weekEnd,
                            existingWeeklyHours, MAX_HOURS_PER_WEEK));
        }

        AllocationDetail detail = AllocationDetail.builder()
                .allocation(allocation)
                .allocationDate(dto.getAllocationDate())
                .hours(dto.getHours())
                .notes(dto.getNotes())
                .build();

        return mapDetailToDTO(detailRepository.save(detail));
    }

    @Transactional
    public AllocationDetailDTO updateAllocationDetail(Long id, AllocationDetailDTO dto) {
        AllocationDetail detail = detailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AllocationDetail", "id", id));

        if (dto.getHours() != null) {
            // Validate excluding current detail hours
            Long employeeId = detail.getAllocation().getEmployeeId();
            LocalDate date = dto.getAllocationDate() != null ? dto.getAllocationDate() : detail.getAllocationDate();

            Double existingDailyHours = detailRepository.getTotalHoursForEmployeeOnDate(employeeId, date);
            double netDaily = existingDailyHours - detail.getHours() + dto.getHours();
            if (netDaily > MAX_HOURS_PER_DAY) {
                throw new BusinessValidationException(
                        String.format("Update would exceed %.1f hours per day limit. Current total would be %.1f hours.",
                                MAX_HOURS_PER_DAY, netDaily));
            }

            LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate weekEnd = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
            Double existingWeeklyHours = detailRepository.getTotalWeeklyHoursForEmployee(employeeId, weekStart, weekEnd);
            double netWeekly = existingWeeklyHours - detail.getHours() + dto.getHours();
            if (netWeekly > MAX_HOURS_PER_WEEK) {
                throw new BusinessValidationException(
                        String.format("Update would exceed %.1f hours per week limit. Current weekly total would be %.1f hours.",
                                MAX_HOURS_PER_WEEK, netWeekly));
            }

            detail.setHours(dto.getHours());
        }
        if (dto.getAllocationDate() != null) detail.setAllocationDate(dto.getAllocationDate());
        if (dto.getNotes() != null) detail.setNotes(dto.getNotes());

        return mapDetailToDTO(detailRepository.save(detail));
    }

    @Transactional
    public void deleteAllocationDetail(Long id) {
        AllocationDetail detail = detailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AllocationDetail", "id", id));
        detail.setActive(false);
        detailRepository.save(detail);
    }

    public List<AllocationDetailDTO> getDetailsByAllocation(Long allocationId) {
        return detailRepository.findByAllocationIdAndActiveTrue(allocationId).stream()
                .map(this::mapDetailToDTO)
                .collect(Collectors.toList());
    }

    // =================== Validation ===================

    private void validateAllocationConstraints(AllocationDTO dto, Long excludeId) {
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessValidationException("End date cannot be before start date");
        }

        if (dto.getHoursPerDay() > MAX_HOURS_PER_DAY) {
            throw new BusinessValidationException(
                    String.format("Hours per day cannot exceed %.1f hours", MAX_HOURS_PER_DAY));
        }

        // Check daily allocation across all projects for the employee
        LocalDate current = dto.getStartDate();
        while (!current.isAfter(dto.getEndDate())) {
            // Skip weekends
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY && current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                Double existingHours = allocationRepository.getTotalHoursForEmployeeOnDate(
                        dto.getEmployeeId(), current);

                if (excludeId != null) {
                    // Subtract hours from the allocation being updated
                    Allocation existing = allocationRepository.findById(excludeId).orElse(null);
                    if (existing != null && !current.isBefore(existing.getStartDate()) && !current.isAfter(existing.getEndDate())) {
                        existingHours -= existing.getHoursPerDay();
                    }
                }

                if (existingHours + dto.getHoursPerDay() > MAX_HOURS_PER_DAY) {
                    throw new BusinessValidationException(
                            String.format("Employee allocation would exceed %.1f hours on %s. " +
                                    "Currently allocated: %.1f hours", MAX_HOURS_PER_DAY, current, existingHours));
                }
            }
            current = current.plusDays(1);
        }

        // Check weekly allocation
        LocalDate weekStart = dto.getStartDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = dto.getEndDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));

        LocalDate currentWeekStart = weekStart;
        while (!currentWeekStart.isAfter(weekEnd)) {
            LocalDate currentWeekEnd = currentWeekStart.plusDays(4); // Monday to Friday
            long workDaysInWeek = Math.min(5, java.time.temporal.ChronoUnit.DAYS.between(
                    currentWeekStart.isBefore(dto.getStartDate()) ? dto.getStartDate() : currentWeekStart,
                    (currentWeekEnd.isAfter(dto.getEndDate()) ? dto.getEndDate() : currentWeekEnd)) + 1);

            double newWeeklyHours = dto.getHoursPerDay() * workDaysInWeek;

            Double existingWeeklyHours = 0.0;
            if (excludeId != null) {
                existingWeeklyHours = allocationRepository.getTotalWeeklyHoursExcluding(
                        dto.getEmployeeId(), currentWeekStart, currentWeekEnd, excludeId);
            } else {
                existingWeeklyHours = allocationRepository.getTotalWeeklyHoursExcluding(
                        dto.getEmployeeId(), currentWeekStart, currentWeekEnd, -1L);
            }

            if (existingWeeklyHours + newWeeklyHours > MAX_HOURS_PER_WEEK) {
                throw new BusinessValidationException(
                        String.format("Employee allocation would exceed %.1f hours in week %s to %s. " +
                                "Currently allocated: %.1f hours, new: %.1f hours",
                                MAX_HOURS_PER_WEEK, currentWeekStart, currentWeekEnd,
                                existingWeeklyHours, newWeeklyHours));
            }

            currentWeekStart = currentWeekStart.plusWeeks(1);
        }
    }

    // =================== Mappers ===================

    private AllocationDTO mapToDTO(Allocation entity) {
        List<AllocationDetailDTO> details = entity.getDetails() != null
                ? entity.getDetails().stream()
                        .filter(d -> d.isActive())
                        .map(this::mapDetailToDTO)
                        .collect(Collectors.toList())
                : null;

        return AllocationDTO.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .projectId(entity.getProjectId())
                .projectScheduleId(entity.getProjectScheduleId())
                .roleName(entity.getRoleName())
                .status(entity.getStatus())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .hoursPerDay(entity.getHoursPerDay())
                .notes(entity.getNotes())
                .active(entity.isActive())
                .details(details)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private AllocationDetailDTO mapDetailToDTO(AllocationDetail entity) {
        return AllocationDetailDTO.builder()
                .id(entity.getId())
                .allocationId(entity.getAllocation().getId())
                .allocationDate(entity.getAllocationDate())
                .hours(entity.getHours())
                .notes(entity.getNotes())
                .active(entity.isActive())
                .build();
    }

    private PagedResponse<AllocationDTO> mapToPagedResponse(Page<Allocation> page) {
        return PagedResponse.<AllocationDTO>builder()
                .content(page.getContent().stream().map(this::mapToDTO).collect(Collectors.toList()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
