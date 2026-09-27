package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.data_models.ERole;
import com.Mediatron.Lab_Management.data_models.Role;
import com.Mediatron.Lab_Management.data_models.User;
import com.Mediatron.Lab_Management.repositories.RoleRepository;
import com.Mediatron.Lab_Management.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @PutMapping("/{userId}/assign-role")
    @PreAuthorize("@delegationService.hasAdminPrivileges(authentication)")
    @Transactional
    public ResponseEntity<?> assignRole(@PathVariable Integer userId, @RequestBody String roleName) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body("User not found.");
        }

        String normalizedRole = roleName == null ? "" : roleName.trim().replace("\"", "");

        ERole newRoleEnum;
        try {
            newRoleEnum = ERole.valueOf(normalizedRole);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid role. Use ROLE_ADMIN, ROLE_MEMBER or ROLE_LAB_HEAD.");
        }

        Role role = roleRepository.findByName(newRoleEnum).orElse(null);
        if (role == null) {
            return ResponseEntity.status(404).body("Role not found in database.");
        }

        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        return ResponseEntity.ok(Map.of(
                "message", "Role updated successfully.",
                "userId", user.getId(),
                "role", newRoleEnum.name()
        ));
    }
}
