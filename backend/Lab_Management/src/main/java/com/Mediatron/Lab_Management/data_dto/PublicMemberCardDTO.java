package com.Mediatron.Lab_Management.data_dto;

import lombok.Data;

@Data
public class PublicMemberCardDTO {
    private Integer id;
    private String photoUrl;
    private String firstName;
    private String lastName;
    private String grade;
    private String contact;
    private String personalSiteUrl;
    private String linkedinUrl;
    private String googleScholarUrl;
}
