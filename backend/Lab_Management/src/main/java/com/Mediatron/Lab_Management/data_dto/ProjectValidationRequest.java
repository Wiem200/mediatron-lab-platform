package com.Mediatron.Lab_Management.data_dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectValidationRequest {

    @NotBlank(message = "Decision is required.")
    private String decision;

    private String comment;
}
