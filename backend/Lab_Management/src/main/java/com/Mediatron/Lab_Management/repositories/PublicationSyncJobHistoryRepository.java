package com.Mediatron.Lab_Management.repositories;

import com.Mediatron.Lab_Management.data_models.PublicationSyncJobHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublicationSyncJobHistoryRepository extends JpaRepository<PublicationSyncJobHistory, Integer> {

    List<PublicationSyncJobHistory> findTop50ByOrderByStartedAtDescIdDesc();

    List<PublicationSyncJobHistory> findTop50ByTargetMemberIdOrderByStartedAtDescIdDesc(Integer targetMemberId);
}