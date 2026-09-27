package com.Mediatron.Lab_Management.data_dto;

import lombok.Data;

@Data
public class MemberPublicationSyncDTO {
    private boolean synchronizationAvailable;
    private String status;
    private String source;
    private String profileUrl;
    private String identifier;
    private String message;
}
