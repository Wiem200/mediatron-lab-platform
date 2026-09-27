package com.Mediatron.Lab_Management.data_dto;

import java.time.LocalDateTime;

public class AdminDelegationStatusDTO {
    private boolean active;
    private LocalDateTime activatedAt;

    public AdminDelegationStatusDTO() {
    }

    public AdminDelegationStatusDTO(boolean active, LocalDateTime activatedAt) {
        this.active = active;
        this.activatedAt = activatedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }
}
