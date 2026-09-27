package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.authentication.JwtUtils;
import com.Mediatron.Lab_Management.authentication.UserPrincipal;
import com.Mediatron.Lab_Management.data_dto.*;
import com.Mediatron.Lab_Management.data_models.*;
import com.Mediatron.Lab_Management.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {
    /**private static final String INSTITUTIONAL_EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@(supcom\\.tn|gmail\\.com)$";**/
    private static final String INSTITUTIONAL_EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@supcom\\.tn$";
    private static final String INSTITUTIONAL_EMAIL_ERROR =
            "Access Denied: Registration is restricted to valid @supcom.tn or @gmail.com emails for testing.";

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private AccountEmailService accountEmailService;
    @Autowired private DelegationService delegationService;

    public APIResponse<JwtResponse> loginUser(LoginRequest loginRequest) {
        String normalizedEmail = loginRequest.getEmail() == null ? null : loginRequest.getEmail().trim().toLowerCase();

        // 1. Fetch user to check status before authenticating
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // 2. CHECK STATUS (Logic from your friend)
        if (user.getStatus() == AccountStatus.PENDING) {
            return new APIResponse<>(false, "Your account is pending Admin approval.", null);
        }
        if (user.getStatus() == AccountStatus.REJECTED) {
            return new APIResponse<>(false, "Your registration has been rejected.", null);
        }

        // 3. Authenticate
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority()).collect(Collectors.toList());

        if (roles.contains("ROLE_ADMIN")) {
            delegationService.deactivateAllDelegations();
        }

        boolean delegationActive = roles.contains("ROLE_LAB_HEAD")
                && delegationService.isDelegationActiveForUser(userDetails.getId());

        return APIResponse.ok(
                "Login successful",
                new JwtResponse(jwt, userDetails.getId(), userDetails.getEmail(), roles, delegationActive)
        );
    }

    @Transactional
    public APIResponse<?> registerUser(User user) {
        String normalizedEmail = user.getEmail() == null ? null : user.getEmail().trim().toLowerCase();
        user.setEmail(normalizedEmail);

        if (normalizedEmail == null || !normalizedEmail.matches(INSTITUTIONAL_EMAIL_REGEX)) {
            return APIResponse.error(INSTITUTIONAL_EMAIL_ERROR, 403);
        }

        Optional<User> existingUserOpt = userRepository.findByEmail(normalizedEmail);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            // Admin-created lab profiles already exist as linked user/member records.
            if (existingUser.getMember() == null) {
                return APIResponse.error("Error: Email is already taken!");
            }

            // Treat the bootstrap password as an unclaimed profile.
            if (!encoder.matches("123456", existingUser.getPassword())) {
                return APIResponse.error("An account with this lab email is already activated. Please sign in.");
            }

            existingUser.setPassword(encoder.encode(user.getPassword()));
            existingUser.setStatus(AccountStatus.APPROVED);
            userRepository.save(existingUser);
            accountEmailService.sendWelcomeEmail(existingUser.getEmail(), buildFullName(existingUser.getMember()));

            String assignedRole = existingUser.getRoles().stream()
                    .findFirst()
                    .map(role -> role.getName().name().replace("ROLE_", "").replace('_', ' '))
                    .orElse("MEMBER");

            return APIResponse.ok("Account created successfully. You exist as a laboratory member. Check your email to access the login link.", null);
        }
        return APIResponse.error("You do not exist as a laboratory member. Registration is not allowed.");
    }

    private String buildFullName(Member member) {
        if (member == null) {
            return "";
        }

        String firstName = member.getFirstName() == null ? "" : member.getFirstName().trim();
        String lastName = member.getLastName() == null ? "" : member.getLastName().trim();
        return (firstName + " " + lastName).trim();
    }
}
