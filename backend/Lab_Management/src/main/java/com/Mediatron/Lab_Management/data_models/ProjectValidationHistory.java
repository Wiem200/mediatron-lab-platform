package com.Mediatron.Lab_Management.data_models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_validation_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectValidationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProjectValidationDecision decision;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "validated_at", nullable = false)
    private LocalDateTime validatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "validator_id", nullable = false)
    private User validator;

    @PrePersist
    void assignValidatedAt() {
        if (validatedAt == null) {
            validatedAt = LocalDateTime.now();
        }
    }
}
