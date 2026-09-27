package com.Mediatron.Lab_Management.repositories;

import com.Mediatron.Lab_Management.data_models.PublicationValidationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublicationValidationHistoryRepository
        extends JpaRepository<PublicationValidationHistory, Integer> {

    List<PublicationValidationHistory> findByPublication_IdOrderByCreatedAtDescIdDesc(Integer publicationId);
}