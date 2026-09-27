package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.data_dto.AdminDelegationStatusDTO;
import com.Mediatron.Lab_Management.services.DelegationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin-delegation")
public class AdminDelegationController {

    private final DelegationService delegationService;

    public AdminDelegationController(DelegationService delegationService) {
        this.delegationService = delegationService;
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AdminDelegationStatusDTO> getStatus(Authentication authentication) {
        return ResponseEntity.ok(delegationService.getStatus(authentication));
    }

    @PostMapping("/activate")
    @PreAuthorize("hasRole('LAB_HEAD')")
    public ResponseEntity<AdminDelegationStatusDTO> activate(Authentication authentication) {
        return ResponseEntity.ok(delegationService.activateDelegation(authentication));
    }

    @PostMapping("/deactivate")
    @PreAuthorize("hasRole('LAB_HEAD')")
    public ResponseEntity<AdminDelegationStatusDTO> deactivate(Authentication authentication) {
        return ResponseEntity.ok(delegationService.deactivateDelegation(authentication));
    }
}
