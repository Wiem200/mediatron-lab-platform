package com.Mediatron.Lab_Management.data_dto;

import com.Mediatron.Lab_Management.data_models.SubscriberStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriberStatusUpdateRequest {
    @NotNull(message = "Status is required.")
    private SubscriberStatus status;
}
