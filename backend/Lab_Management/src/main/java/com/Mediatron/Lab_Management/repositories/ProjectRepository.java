package com.Mediatron.Lab_Management.repositories;

import com.Mediatron.Lab_Management.data_models.Project;
import com.Mediatron.Lab_Management.data_models.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {

    List<Project> findAllByOrderByTitleAsc();

    List<Project> findByStatusOrderByTitleAsc(ProjectStatus status);

    Optional<Project> findByIdAndStatus(Integer id, ProjectStatus status);

    List<Project> findByResponsibleMember_Id(Integer memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE projects SET responsible_member_id = NULL WHERE responsible_member_id = :memberId", nativeQuery = true)
    int clearResponsibleMemberByMemberId(@Param("memberId") Integer memberId);
}