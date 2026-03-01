package com.eretain.project.service;

import com.eretain.common.dto.PagedResponse;
import com.eretain.common.enums.ProjectStatus;
import com.eretain.common.exception.BusinessValidationException;
import com.eretain.common.exception.ResourceNotFoundException;
import com.eretain.project.dto.ProjectDTO;
import com.eretain.project.dto.ProjectScheduleDTO;
import com.eretain.project.entity.Project;
import com.eretain.project.entity.ProjectSchedule;
import com.eretain.project.repository.ProjectRepository;
import com.eretain.project.repository.ProjectScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectScheduleRepository scheduleRepository;

    public PagedResponse<ProjectDTO> getAllProjects(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Project> projects = projectRepository.findByActiveTrue(pageable);
        return mapToPagedResponse(projects);
    }

    public ProjectDTO getProjectById(Long id) {
        return mapToDTO(projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id)));
    }

    public ProjectDTO getProjectByCode(String code) {
        return mapToDTO(projectRepository.findByProjectCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "code", code)));
    }

    public List<ProjectDTO> getActiveProjects() {
        return projectRepository.findByStatusIn(List.of(ProjectStatus.ACTIVE)).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectDTO createProject(ProjectDTO dto) {
        if (projectRepository.existsByProjectCode(dto.getProjectCode())) {
            throw new BusinessValidationException("Project code already exists: " + dto.getProjectCode());
        }

        Project project = Project.builder()
                .projectCode(dto.getProjectCode())
                .name(dto.getName())
                .description(dto.getDescription())
                .projectType(dto.getProjectType())
                .billabilityCategory(dto.getBillabilityCategory())
                .status(dto.getStatus() != null ? dto.getStatus() : ProjectStatus.PROPOSED)
                .clientName(dto.getClientName())
                .deliveryUnitId(dto.getDeliveryUnitId())
                .projectManagerId(dto.getProjectManagerId())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .budget(dto.getBudget())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "USD")
                .build();

        return mapToDTO(projectRepository.save(project));
    }

    @Transactional
    public ProjectDTO updateProject(Long id, ProjectDTO dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));

        if (dto.getName() != null) project.setName(dto.getName());
        if (dto.getDescription() != null) project.setDescription(dto.getDescription());
        if (dto.getProjectType() != null) project.setProjectType(dto.getProjectType());
        if (dto.getBillabilityCategory() != null) project.setBillabilityCategory(dto.getBillabilityCategory());
        if (dto.getStatus() != null) project.setStatus(dto.getStatus());
        if (dto.getClientName() != null) project.setClientName(dto.getClientName());
        if (dto.getDeliveryUnitId() != null) project.setDeliveryUnitId(dto.getDeliveryUnitId());
        if (dto.getProjectManagerId() != null) project.setProjectManagerId(dto.getProjectManagerId());
        if (dto.getStartDate() != null) project.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) project.setEndDate(dto.getEndDate());
        if (dto.getBudget() != null) project.setBudget(dto.getBudget());
        if (dto.getCurrency() != null) project.setCurrency(dto.getCurrency());

        return mapToDTO(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        project.setActive(false);
        project.setStatus(ProjectStatus.CANCELLED);
        projectRepository.save(project);
    }

    // =================== Schedule ===================

    public List<ProjectScheduleDTO> getProjectSchedules(Long projectId) {
        return scheduleRepository.findByProjectIdOrderBySortOrderAsc(projectId).stream()
                .map(this::mapScheduleToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectScheduleDTO createSchedule(ProjectScheduleDTO dto) {
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", dto.getProjectId()));

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessValidationException("End date cannot be before start date");
        }

        ProjectSchedule schedule = ProjectSchedule.builder()
                .project(project)
                .phaseName(dto.getPhaseName())
                .description(dto.getDescription())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .plannedHours(dto.getPlannedHours())
                .sortOrder(dto.getSortOrder())
                .build();

        return mapScheduleToDTO(scheduleRepository.save(schedule));
    }

    @Transactional
    public ProjectScheduleDTO updateSchedule(Long id, ProjectScheduleDTO dto) {
        ProjectSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectSchedule", "id", id));

        if (dto.getPhaseName() != null) schedule.setPhaseName(dto.getPhaseName());
        if (dto.getDescription() != null) schedule.setDescription(dto.getDescription());
        if (dto.getStartDate() != null) schedule.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) schedule.setEndDate(dto.getEndDate());
        if (dto.getPlannedHours() != null) schedule.setPlannedHours(dto.getPlannedHours());
        if (dto.getSortOrder() != null) schedule.setSortOrder(dto.getSortOrder());

        return mapScheduleToDTO(scheduleRepository.save(schedule));
    }

    @Transactional
    public void deleteSchedule(Long id) {
        ProjectSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectSchedule", "id", id));
        schedule.setActive(false);
        scheduleRepository.save(schedule);
    }

    // =================== Mappers ===================

    private ProjectDTO mapToDTO(Project entity) {
        List<ProjectScheduleDTO> schedules = entity.getSchedules() != null
                ? entity.getSchedules().stream().map(this::mapScheduleToDTO).collect(Collectors.toList())
                : null;

        return ProjectDTO.builder()
                .id(entity.getId())
                .projectCode(entity.getProjectCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .projectType(entity.getProjectType())
                .billabilityCategory(entity.getBillabilityCategory())
                .status(entity.getStatus())
                .clientName(entity.getClientName())
                .deliveryUnitId(entity.getDeliveryUnitId())
                .projectManagerId(entity.getProjectManagerId())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .budget(entity.getBudget())
                .currency(entity.getCurrency())
                .active(entity.isActive())
                .schedules(schedules)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ProjectScheduleDTO mapScheduleToDTO(ProjectSchedule entity) {
        return ProjectScheduleDTO.builder()
                .id(entity.getId())
                .projectId(entity.getProject().getId())
                .phaseName(entity.getPhaseName())
                .description(entity.getDescription())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .plannedHours(entity.getPlannedHours())
                .sortOrder(entity.getSortOrder())
                .active(entity.isActive())
                .build();
    }

    private PagedResponse<ProjectDTO> mapToPagedResponse(Page<Project> page) {
        return PagedResponse.<ProjectDTO>builder()
                .content(page.getContent().stream().map(this::mapToDTO).collect(Collectors.toList()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
