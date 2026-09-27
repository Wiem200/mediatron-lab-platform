package com.Mediatron.Lab_Management.exceptions;

public class DuplicatePublicationDoiException extends RuntimeException {

    private final String doi;

    public DuplicatePublicationDoiException(String doi) {
        super("A publication with DOI '" + doi + "' already exists.");
        this.doi = doi;
    }

    public DuplicatePublicationDoiException(String doi, String message) {
        super(message);
        this.doi = doi;
    }

    public String getDoi() {
        return doi;
    }
}