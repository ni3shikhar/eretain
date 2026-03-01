package com.eretain.project.repository;

import com.eretain.project.entity.ProjectSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectScheduleRepository extends JpaRepository<ProjectSchedule, Long> {
    List<ProjectSchedule> findByProjectIdOrderBySortOrderAsc(Long projectId);
}
