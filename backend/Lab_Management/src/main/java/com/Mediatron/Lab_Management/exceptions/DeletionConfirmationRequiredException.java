package com.Mediatron.Lab_Management.exceptions;

public class DeletionConfirmationRequiredException extends RuntimeException {
    private final Integer memberId;

    public DeletionConfirmationRequiredException(Integer memberId) {
        super("Deletion requires explicit confirmation. Retry this request with ?confirm=true.");
        this.memberId = memberId;
    }

    public Integer getMemberId() {
        return memberId;
    }
}
