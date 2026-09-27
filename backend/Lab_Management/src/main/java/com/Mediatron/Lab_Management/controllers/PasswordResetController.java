package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.data_dto.APIResponse;
import com.Mediatron.Lab_Management.data_dto.ForgotPasswordRequest;
import com.Mediatron.Lab_Management.data_dto.ResetPasswordRequest;
import com.Mediatron.Lab_Management.services.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<APIResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        APIResponse<Void> response = passwordResetService.requestPasswordReset(request);
        HttpStatus status = response.getStatus() == null
                ? HttpStatus.OK
                : HttpStatus.valueOf(response.getStatus());

        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<APIResponse<Void>> resetPassword(
            @RequestParam("token") String token,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        APIResponse<Void> response = passwordResetService.resetPassword(token, request);
        HttpStatus status = response.getStatus() == null
                ? HttpStatus.OK
                : HttpStatus.valueOf(response.getStatus());

        return ResponseEntity.status(status).body(response);
    }
}