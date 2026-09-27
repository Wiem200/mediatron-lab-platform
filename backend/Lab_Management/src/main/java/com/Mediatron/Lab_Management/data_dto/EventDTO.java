package com.Mediatron.Lab_Management.data_dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventDTO {
    private Integer id;
    private String title;
    private String description;
    private LocalDateTime eventDate;
    private String location;
    private String responsiblePerson;
    private String status;
}