package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.exceptions.DeletionConfirmationRequiredException;
import com.Mediatron.Lab_Management.exceptions.PhDStudentDeletionConfirmationRequiredException;
import com.Mediatron.Lab_Management.exceptions.ProjectDeletionConfirmationRequiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildBody(
                HttpStatus.BAD_REQUEST,
                "Database constraint violation.",
                null
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildBody(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                null
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Validation failed.",
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        String message = validationErrors.values().stream()
                .findFirst()
                .orElse("Validation failed.");

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("validationErrors", validationErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildBody(
                HttpStatus.BAD_REQUEST,
                message,
                extra
        ));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildBody(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                null
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(buildBody(
                HttpStatus.FORBIDDEN,
                ex.getMessage() != null ? ex.getMessage() : "Access denied.",
                null
        ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(buildBody(
                HttpStatus.UNAUTHORIZED,
                "Wrong password try again",
                null
        ));
    }

    @ExceptionHandler(DeletionConfirmationRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleDeletionConfirmationRequired(DeletionConfirmationRequiredException ex) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("confirmationRequired", true);
        extra.put("memberId", ex.getMemberId());
        extra.put("confirmParameter", "confirm=true");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(buildBody(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                extra
        ));
    }

    @ExceptionHandler(ProjectDeletionConfirmationRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleProjectDeletionConfirmationRequired(ProjectDeletionConfirmationRequiredException ex) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("confirmationRequired", true);
        extra.put("projectId", ex.getProjectId());
        extra.put("confirmParameter", "confirm=true");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(buildBody(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                extra
        ));
    }

    @ExceptionHandler(PhDStudentDeletionConfirmationRequiredException.class)
    public ResponseEntity<Map<String, Object>> handlePhdStudentDeletionConfirmationRequired(PhDStudentDeletionConfirmationRequiredException ex) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("confirmationRequired", true);
        extra.put("phdStudentId", ex.getPhdStudentId());
        extra.put("confirmParameter", "confirm=true");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(buildBody(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                extra
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleGatewayErrors(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(buildBody(
                HttpStatus.BAD_GATEWAY,
                ex.getMessage(),
                null
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled server exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildBody(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                null
        ));
    }

    private Map<String, Object> buildBody(HttpStatus status, String message, Map<String, Object> extra) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        if (extra != null) {
            body.putAll(extra);
        }

        return body;
    }
}
