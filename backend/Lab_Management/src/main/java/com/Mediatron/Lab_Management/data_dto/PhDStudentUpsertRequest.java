package com.Mediatron.Lab_Management.data_dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PhDStudentUpsertRequest {

    @NotBlank(message = "First name is required.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    private String lastName;

    private String photoUrl;

    @NotBlank(message = "Thesis topic is required.")
    private String thesisTopic;

    @NotNull(message = "Registration year is required.")
    @Min(value = 2000, message = "Registration year must be at least 2000.")
    private Integer registrationYear;

    @NotBlank(message = "Research team is required.")
    private String researchTeam;

    @NotNull(message = "Primary supervisor is required.")
    private Integer supervisorId;

    private String coSupervisor;

    private List<Integer> publicationIds = new ArrayList<>();
}
