package com.Mediatron.Lab_Management.repositories;

import com.Mediatron.Lab_Management.data_models.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
}
