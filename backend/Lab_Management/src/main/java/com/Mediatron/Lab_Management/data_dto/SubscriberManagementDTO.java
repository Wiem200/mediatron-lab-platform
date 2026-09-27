package com.Mediatron.Lab_Management.data_dto;

import com.Mediatron.Lab_Management.data_models.SubscriberStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriberManagementDTO {
    private Integer id;
    private String email;
    private SubscriberStatus status;
    private LocalDateTime subscribedAt;
}
