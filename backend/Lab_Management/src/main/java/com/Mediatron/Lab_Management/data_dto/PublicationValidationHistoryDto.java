package com.Mediatron.Lab_Management.data_dto;

import com.Mediatron.Lab_Management.data_models.PublicationStatus;

import java.time.LocalDateTime;

public class PublicationValidationHistoryDto {

    private Integer id;
    private String actorEmail;
    private String actorRole;
    private String actionType;
    private PublicationStatus previousStatus;
    private PublicationStatus newStatus;
    private String note;
    private LocalDateTime createdAt;

    public PublicationValidationHistoryDto() {
    }

    public PublicationValidationHistoryDto(
            Integer id,
            String actorEmail,
            String actorRole,
            String actionType,
            PublicationStatus previousStatus,
            PublicationStatus newStatus,
            String note,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.actorEmail = actorEmail;
        this.actorRole = actorRole;
        this.actionType = actionType;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.note = note;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public PublicationStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(PublicationStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public PublicationStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(PublicationStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}