package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_dto.APIResponse;
import com.Mediatron.Lab_Management.data_dto.ForgotPasswordRequest;
import com.Mediatron.Lab_Management.data_dto.ResetPasswordRequest;
import com.Mediatron.Lab_Management.data_models.PasswordResetToken;
import com.Mediatron.Lab_Management.data_models.User;
import com.Mediatron.Lab_Management.repositories.PasswordResetTokenRepository;
import com.Mediatron.Lab_Management.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final String FORGOT_PASSWORD_RESPONSE =
            "If an account exists for that email, a reset link has been sent.";
    private static final String INVALID_TOKEN_MESSAGE =
            "Invalid or expired password reset token.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AccountEmailService accountEmailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            AccountEmailService accountEmailService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.accountEmailService = accountEmailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public APIResponse<Void> requestPasswordReset(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();

        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            passwordResetTokenRepository.deleteByUser_Id(user.getId());

            String token = UUID.randomUUID().toString();
            passwordResetTokenRepository.save(new PasswordResetToken(token, user));
            accountEmailService.sendResetPasswordEmail(user.getEmail(), token);
        }

        return APIResponse.ok(FORGOT_PASSWORD_RESPONSE, null);
    }

    @Transactional
    public APIResponse<Void> resetPassword(String rawToken, ResetPasswordRequest request) {
        String token = rawToken == null ? "" : rawToken.trim();

        Optional<PasswordResetToken> resetTokenOpt = passwordResetTokenRepository.findByToken(token);
        if (resetTokenOpt.isEmpty()) {
            return APIResponse.error(INVALID_TOKEN_MESSAGE, 400);
        }

        PasswordResetToken resetToken = resetTokenOpt.get();
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            return APIResponse.error(INVALID_TOKEN_MESSAGE, 400);
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);

        return APIResponse.ok("Password updated successfully.", null);
    }
}
