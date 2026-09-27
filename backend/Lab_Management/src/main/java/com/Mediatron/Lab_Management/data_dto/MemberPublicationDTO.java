package com.Mediatron.Lab_Management.data_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberPublicationDTO {
    private Integer id;
    private String source;
    private String title;
    private String authorsText;
    private Integer publicationYear;
    private String doi;
    private String externalUrl;
    private String type;
    private String status;
}
