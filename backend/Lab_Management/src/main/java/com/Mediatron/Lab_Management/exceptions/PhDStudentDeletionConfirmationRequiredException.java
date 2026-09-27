package com.Mediatron.Lab_Management.exceptions;

public class PhDStudentDeletionConfirmationRequiredException extends RuntimeException {
    private final Integer phdStudentId;

    public PhDStudentDeletionConfirmationRequiredException(Integer phdStudentId) {
        super("Deletion confirmation required for PhD student " + phdStudentId + ". Re-submit with confirm=true.");
        this.phdStudentId = phdStudentId;
    }

    public Integer getPhdStudentId() {
        return phdStudentId;
    }
}
