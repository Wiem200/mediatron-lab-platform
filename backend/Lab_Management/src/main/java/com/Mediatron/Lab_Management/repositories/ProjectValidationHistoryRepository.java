package com.Mediatron.Lab_Management.repositories;

import com.Mediatron.Lab_Management.data_models.ProjectValidationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectValidationHistoryRepository extends JpaRepository<ProjectValidationHistory, Long> {
    Optional<ProjectValidationHistory> findTopByProjectIdOrderByValidatedAtDescIdDesc(Integer projectId);
}
