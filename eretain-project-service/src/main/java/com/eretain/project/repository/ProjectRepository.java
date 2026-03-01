package com.eretain.project.repository;

import com.eretain.common.enums.ProjectStatus;
import com.eretain.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByProjectCode(String projectCode);
    boolean existsByProjectCode(String projectCode);
    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);
    Page<Project> findByActiveTrue(Pageable pageable);
    List<Project> findByDeliveryUnitId(Long deliveryUnitId);
    List<Project> findByProjectManagerId(Long managerId);
    List<Project> findByStatusIn(List<ProjectStatus> statuses);
}
