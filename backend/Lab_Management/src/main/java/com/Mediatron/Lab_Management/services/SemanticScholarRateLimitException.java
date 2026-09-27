package com.Mediatron.Lab_Management.services;

public class SemanticScholarRateLimitException extends RuntimeException {
    public SemanticScholarRateLimitException(String message) {
        super(message);
    }
}