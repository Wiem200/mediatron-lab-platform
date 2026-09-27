package com.Mediatron.Lab_Management.data_dto;

import jakarta.validation.constraints.NotBlank;

public class EventValidationRequest {

    @NotBlank(message = "Action is required")
    private String action;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
