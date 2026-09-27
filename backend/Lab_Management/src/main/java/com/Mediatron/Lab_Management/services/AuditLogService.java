package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_models.AuditLog;
import com.Mediatron.Lab_Management.data_models.User;
import com.Mediatron.Lab_Management.repositories.AuditLogRepository;
import com.Mediatron.Lab_Management.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void logAction(Integer actorId, String action, Integer targetId) {
        if (actorId == null || action == null || action.isBlank()) {
            return;
        }

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Actor not found."));

        AuditLog auditLog = new AuditLog();
        auditLog.setActor(actor);
        auditLog.setAction(action);
        auditLog.setTargetId(targetId);
        auditLogRepository.save(auditLog);
    }
}
