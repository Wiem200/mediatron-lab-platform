package com.Mediatron.Lab_Management.repositories;

import com.Mediatron.Lab_Management.data_models.AdminDelegation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminDelegationRepository extends JpaRepository<AdminDelegation, Integer> {
    Optional<AdminDelegation> findByLabHeadId(Integer labHeadId);

    boolean existsByLabHeadIdAndActiveTrue(Integer labHeadId);

    @Modifying
    @Query("update AdminDelegation d set d.active = false where d.active = true")
    int deactivateAllActive();
}
