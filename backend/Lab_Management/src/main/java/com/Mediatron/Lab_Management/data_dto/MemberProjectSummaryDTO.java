package com.Mediatron.Lab_Management.data_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberProjectSummaryDTO {
    private Integer id;
    private String title;
    private String description;
    private String duration;
}
