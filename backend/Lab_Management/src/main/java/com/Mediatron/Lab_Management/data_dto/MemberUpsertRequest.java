package com.Mediatron.Lab_Management.data_dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MemberUpsertRequest {
    @NotBlank(message = "First name is required.")
    @Size(max = 100, message = "First name must be 100 characters or fewer.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    @Size(max = 100, message = "Last name must be 100 characters or fewer.")
    private String lastName;

    @NotBlank(message = "Grade is required.")
    @Size(max = 100, message = "Grade must be 100 characters or fewer.")
    private String grade;

    @NotBlank(message = "Contact email is required.")
    @Email(message = "Contact email must be a valid email address.")
    @Size(max = 150, message = "Contact email must be 150 characters or fewer.")
    private String email;

    @NotBlank(message = "Contact phone is required.")
    @Size(max = 50, message = "Contact phone must be 50 characters or fewer.")
    private String phone;

    @Size(max = 2048, message = "Photo URL must be 2048 characters or fewer.")
    private String photoUrl;

    @Size(max = 2048, message = "Personal website must be 2048 characters or fewer.")
    private String personalSiteUrl;

    @Size(max = 2048, message = "LinkedIn profile URL must be 2048 characters or fewer.")
    private String linkedinUrl;

    @Size(max = 2048, message = "Google Scholar profile URL must be 2048 characters or fewer.")
    private String googleScholarUrl;

    @Size(max = 64, message = "ORCID iD must be 64 characters or fewer.")
    private String orcidId;

    @Size(max = 5000, message = "Biography must be 5000 characters or fewer.")
    private String bio;

    @NotEmpty(message = "At least one research axis is required.")
    private List<@NotBlank(message = "Research axis entries cannot be blank.") @Size(max = 255, message = "Research axis must be 255 characters or fewer.") String> researchAxes = new ArrayList<>();

    private List<Integer> projectIds = new ArrayList<>();
    private List<Integer> supervisedStudentIds = new ArrayList<>();
}
