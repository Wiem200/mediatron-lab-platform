package com.Mediatron.Lab_Management.data_dto;

import com.Mediatron.Lab_Management.data_models.ProjectStatus;
import com.Mediatron.Lab_Management.data_models.ProjectValidationDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectDTO {
    private Integer id;

    @NotBlank(message = "Project title is required.")
    private String title;

    @NotBlank(message = "Project description is required.")
    private String description;

    @NotBlank(message = "Project duration is required.")
    private String duration;

    private String funding;
    private List<String> keywords = new ArrayList<>();
    private String githubLink;
    private String videoLink;
    private String pdfLink;
    private String posterLink;
    private ProjectStatus status;
    private ProjectValidationDecision latestValidationDecision;
    private String latestValidationComment;
    private LocalDateTime latestValidatedAt;
    private Integer latestValidatorId;
    private String latestValidatorEmail;

    @NotNull(message = "Responsible member is required.")
    private Integer responsibleMemberId;

    private String responsibleMemberName;
    private String responsibleMemberGrade;
    private String responsibleMemberPhotoUrl;
    private String responsibleName;
    private List<Integer> memberIds = new ArrayList<>();
    private List<String> memberNames = new ArrayList<>();
    private List<String> involvedMembersNames = new ArrayList<>();
    private List<Integer> phdStudentIds = new ArrayList<>();
    private List<String> phdStudentNames = new ArrayList<>();
    private List<String> involvedPhDsNames = new ArrayList<>();
    private ProjectDocumentLinksDTO documentLinks = new ProjectDocumentLinksDTO();
}
