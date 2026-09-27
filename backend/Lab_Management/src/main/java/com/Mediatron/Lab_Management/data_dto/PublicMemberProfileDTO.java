package com.Mediatron.Lab_Management.data_dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
public class PublicMemberProfileDTO {
    private Integer id;
    private String photoUrl;
    private String firstName;
    private String lastName;
    private String grade;
    private String contact;
    private String personalSiteUrl;
    private String linkedinUrl;
    private String googleScholarUrl;
    private String bio;
    private Set<String> researchAxes = new LinkedHashSet<>();
    private List<MemberPublicationDTO> publications = new ArrayList<>();
    private List<MemberProjectSummaryDTO> currentProjects = new ArrayList<>();
    private List<PhDStudentDTO> supervisions = new ArrayList<>();
    private MemberPublicationSyncDTO publicationSync;
}
