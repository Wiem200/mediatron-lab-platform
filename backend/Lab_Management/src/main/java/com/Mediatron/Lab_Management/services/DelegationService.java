package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.authentication.UserPrincipal;
import com.Mediatron.Lab_Management.data_dto.AdminDelegationStatusDTO;
import com.Mediatron.Lab_Management.data_models.AdminDelegation;
import com.Mediatron.Lab_Management.data_models.User;
import com.Mediatron.Lab_Management.repositories.AdminDelegationRepository;
import com.Mediatron.Lab_Management.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service("delegationService")
public class DelegationService {

    private final AdminDelegationRepository adminDelegationRepository;
    private final UserRepository userRepository;

    public DelegationService(AdminDelegationRepository adminDelegationRepository, UserRepository userRepository) {
        this.adminDelegationRepository = adminDelegationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AdminDelegationStatusDTO getStatus(Authentication authentication) {
        Integer userId = extractUserId(authentication);
        if (userId == null || !hasAuthority(authentication, "ROLE_LAB_HEAD")) {
            return new AdminDelegationStatusDTO(false, null);
        }

        return adminDelegationRepository.findByLabHeadId(userId)
                .map(this::toStatusDto)
                .orElseGet(() -> new AdminDelegationStatusDTO(false, null));
    }

    @Transactional
    public AdminDelegationStatusDTO activateDelegation(Authentication authentication) {
        User user = requireLabHead(authentication);
        AdminDelegation delegation = adminDelegationRepository.findByLabHeadId(user.getId())
                .orElseGet(() -> {
                    AdminDelegation item = new AdminDelegation();
                    item.setLabHead(user);
                    return item;
                });

        delegation.setActive(true);
        delegation.setActivatedAt(LocalDateTime.now());
        return toStatusDto(adminDelegationRepository.save(delegation));
    }

    @Transactional
    public AdminDelegationStatusDTO deactivateDelegation(Authentication authentication) {
        User user = requireLabHead(authentication);
        AdminDelegation delegation = adminDelegationRepository.findByLabHeadId(user.getId())
                .orElseGet(() -> {
                    AdminDelegation item = new AdminDelegation();
                    item.setLabHead(user);
                    item.setActivatedAt(null);
                    return item;
                });

        delegation.setActive(false);
        return toStatusDto(adminDelegationRepository.save(delegation));
    }

    @Transactional(readOnly = true)
    public boolean isDelegationActive() {
        return isDelegationActive(SecurityContextHolder.getContext().getAuthentication());
    }

    @Transactional(readOnly = true)
    public boolean isDelegationActive(Authentication authentication) {
        Integer userId = extractUserId(authentication);
        return userId != null
                && hasAuthority(authentication, "ROLE_LAB_HEAD")
                && adminDelegationRepository.existsByLabHeadIdAndActiveTrue(userId);
    }

    @Transactional(readOnly = true)
    public boolean isDelegationActiveForUser(Integer userId) {
        return userId != null && adminDelegationRepository.existsByLabHeadIdAndActiveTrue(userId);
    }

    @Transactional(readOnly = true)
    public boolean hasAdminPrivileges(Authentication authentication) {
        return hasAuthority(authentication, "ROLE_ADMIN") || isDelegationActive(authentication);
    }

    @Transactional
    public void deactivateAllDelegations() {
        adminDelegationRepository.deactivateAllActive();
    }

    private AdminDelegationStatusDTO toStatusDto(AdminDelegation delegation) {
        LocalDateTime activatedAt = delegation.isActive() ? delegation.getActivatedAt() : null;
        return new AdminDelegationStatusDTO(delegation.isActive(), activatedAt);
    }

    private User requireLabHead(Authentication authentication) {
        Integer userId = extractUserId(authentication);
        if (userId == null || !hasAuthority(authentication, "ROLE_LAB_HEAD")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Lab Heads can manage delegation mode.");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private Integer extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }

        return null;
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null || authority == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }
}
