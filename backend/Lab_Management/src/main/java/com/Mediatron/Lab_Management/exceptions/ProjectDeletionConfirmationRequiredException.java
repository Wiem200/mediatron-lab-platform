package com.Mediatron.Lab_Management.exceptions;

public class ProjectDeletionConfirmationRequiredException extends RuntimeException {
    private final Integer projectId;

    public ProjectDeletionConfirmationRequiredException(Integer projectId) {
        super("Project deletion requires explicit confirmation.");
        this.projectId = projectId;
    }

    public Integer getProjectId() {
        return projectId;
    }
}
