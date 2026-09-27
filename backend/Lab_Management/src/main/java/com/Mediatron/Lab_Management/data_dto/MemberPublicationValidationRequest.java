package com.Mediatron.Lab_Management.data_dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MemberPublicationValidationRequest {
    @NotBlank
    private String action;
}
