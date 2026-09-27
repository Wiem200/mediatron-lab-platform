package com.Mediatron.Lab_Management.data_dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
public class MemberDTO {
    private Integer id;
    private Integer userId;
    private String lab;
    private String photoUrl;
    private String firstName;
    private String lastName;
    private String grade;
    private String email;
    private String phone;
    private String contact;
    private String role;
    private String personalSiteUrl;
    private String linkedinUrl;
    private String googleScholarUrl;
    private Set<String> researchAxes = new LinkedHashSet<>();
    private String bio;
    private String orcidId;
    private List<PhDStudentDTO> supervisedStudents = new ArrayList<>();
    private List<MemberProjectSummaryDTO> currentProjects = new ArrayList<>();
    private List<MemberPublicationDTO> publications = new ArrayList<>();
    private MemberPublicationSyncDTO publicationSync;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
