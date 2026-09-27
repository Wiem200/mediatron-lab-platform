package com.Mediatron.Lab_Management.data_dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PhDStudentDTO {
    private Integer id;
    private String photoUrl;
    private String firstName;
    private String lastName;
    private String fullName;
    private Integer supervisorId;
    private String supervisorName;
    private String coSupervisor;
    private String thesisTopic;
    private Integer registrationYear;
    private String researchTeam;
    private List<Integer> publicationIds = new ArrayList<>();
    private List<PhDStudentPublicationDTO> publications = new ArrayList<>();
}
