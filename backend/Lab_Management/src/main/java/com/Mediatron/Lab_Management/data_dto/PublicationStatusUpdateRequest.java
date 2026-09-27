package com.Mediatron.Lab_Management.data_dto;

import com.Mediatron.Lab_Management.data_models.PublicationStatus;

public class PublicationStatusUpdateRequest {

    private PublicationStatus status;
    private String note;

    public PublicationStatus getStatus() {
        return status;
    }

    public void setStatus(PublicationStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}