package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.data_dto.*;
import com.Mediatron.Lab_Management.data_models.User;
import com.Mediatron.Lab_Management.services.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authenticationService.loginUser(loginRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user) {
        APIResponse<?> response = authenticationService.registerUser(user);
        HttpStatus status = response.getStatus() == null
                ? HttpStatus.OK
                : HttpStatus.valueOf(response.getStatus());
        return ResponseEntity.status(status).body(response);
    }
}
