package com.Mediatron.Lab_Management.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AccountEmailService {

    private static final Logger log = LoggerFactory.getLogger(AccountEmailService.class);

    @Nullable
    private final JavaMailSender mailSender;

    @Value("${app.frontend.login-url}")
    private String loginUrl;

    @Value("${app.frontend.reset-password-url}")
    private String resetPasswordUrl;

    @Value("${app.frontend.publications-url:http://localhost:5173/my-publications}")
    private String publicationsUrl;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.reply-to:${app.mail.from}}")
    private String replyToEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    public AccountEmailService(@Nullable JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendWelcomeEmail(String recipientEmail, String fullName) {
        if (mailSender == null) {
            log.warn("Skipping welcome email for {} because SMTP mail is not configured.", recipientEmail);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setFrom(fromEmail);
        message.setReplyTo(replyToEmail);
        message.setSubject("Your MEDIATRON account has been created");
        message.setText(buildWelcomeBody(fullName));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Failed to send welcome email to {}: {}", recipientEmail, ex.getMessage());
        }
    }

    @Async
    public void sendSubscriberWelcomeEmail(String recipientEmail) {
        if (mailSender == null) {
            log.warn("Skipping subscriber welcome email for {} because SMTP mail is not configured.", recipientEmail);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setFrom(fromEmail);
        message.setReplyTo(replyToEmail);
        message.setSubject("Welcome to MEDIATRON updates");
        message.setText(buildSubscriberWelcomeBody());

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Failed to send subscriber welcome email to {}: {}", recipientEmail, ex.getMessage());
        }
    }

    @Async
    public void sendResetPasswordEmail(String recipientEmail, String token) {
        if (mailSender == null) {
            log.warn("Skipping password reset email for {} because SMTP mail is not configured.", recipientEmail);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setFrom(fromEmail);
        message.setReplyTo(replyToEmail);
        message.setSubject("Reset your MEDIATRON password");
        message.setText(buildPasswordResetBody(token));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Failed to send password reset email to {}: {}", recipientEmail, ex.getMessage());
        }
    }

    @Async
    public void sendPublicationDetectedEmail(
            String recipientEmail,
            String fullName,
            String publicationTitle,
            String authorsText,
            String sourceLink
    ) {
        if (mailSender == null) {
            log.warn("Skipping publication notification email for {} because SMTP mail is not configured.", recipientEmail);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setFrom(fromEmail);
        message.setReplyTo(replyToEmail);
        message.setSubject("New publication detected for your MEDIATRON profile");
        message.setText(buildPublicationDetectedBody(fullName, publicationTitle, authorsText, sourceLink));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Failed to send publication notification email to {}: {}", recipientEmail, ex.getMessage());
        }
    }

    @Async
    public void sendSubscriberContentPublishedEmail(
            String recipientEmail,
            String contentTypeLabel,
            String title,
            String summary,
            String contentUrl,
            String unsubscribeUrl
    ) {
        if (mailSender == null) {
            log.warn("Skipping subscriber content notification for {} because SMTP mail is not configured.", recipientEmail);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setFrom(fromEmail);
        message.setReplyTo(replyToEmail);
        message.setSubject(buildContentPublishedSubject(contentTypeLabel, title));
        message.setText(buildContentPublishedBody(contentTypeLabel, title, summary, contentUrl, unsubscribeUrl));

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Failed to send subscriber content notification to {}: {}", recipientEmail, ex.getMessage());
        }
    }

    private String buildWelcomeBody(String fullName) {
        String safeName = fullName == null || fullName.isBlank() ? "there" : fullName;

        return "Hello " + safeName + ",\n\n"
                + "Your MEDIATRON account was created successfully.\n"
                + "You can now sign in using the link below:\n"
                + loginUrl + "\n\n"
                + "Please do not reply to this email.\n\n"
                + "Regards,\n"
                + fromName;
    }

    private String buildSubscriberWelcomeBody() {
        return "Hello,\n\n"
                + "Welcome to MEDIATRON Laboratory.\n"
                + "Thank you for subscribing to our updates.\n"
                + "You will now receive news about our research activities, publications, and academic events.\n\n"
                + "You can visit MEDIATRON here:\n"
                + loginUrl + "\n\n"
                + "Please do not reply to this email.\n\n"
                + "Best regards,\n"
                + fromName;
    }

    private String buildPasswordResetBody(String token) {
        return "Hello,\n\n"
                + "We received a request to reset your MEDIATRON password.\n"
                + "Use the link below to choose a new password:\n"
                + resetPasswordUrl + "?token=" + token + "\n\n"
                + "This link expires in 1 hour.\n"
                + "If you did not request this change, you can ignore this email.\n\n"
                + "Regards,\n"
                + fromName;
    }

    private String buildPublicationDetectedBody(
            String fullName,
            String publicationTitle,
            String authorsText,
            String sourceLink
    ) {
        String safeName = fullName == null || fullName.isBlank() ? "there" : fullName;
        String safeTitle = publicationTitle == null || publicationTitle.isBlank()
                ? "Untitled publication"
                : publicationTitle;
        String safeAuthors = authorsText == null || authorsText.isBlank()
                ? "Authors not available"
                : authorsText;

        String body = "Hello " + safeName + ",\n\n"
                + "A publication related to your MEDIATRON profile has been detected.\n\n"
                + "Title:\n"
                + safeTitle + "\n\n"
                + "Authors:\n"
                + safeAuthors + "\n\n";

        if (sourceLink != null && !sourceLink.isBlank()) {
            body += "Source / DOI link:\n"
                    + sourceLink + "\n\n";
        }

        body += "Please review and validate this publication from your dashboard:\n"
                + publicationsUrl + "\n\n"
                + "If this publication belongs to you, approve it. If it was linked by mistake, reject it.\n\n"
                + "Regards,\n"
                + fromName;

        return body;
    }

    private String buildContentPublishedSubject(String contentTypeLabel, String title) {
        String safeType = contentTypeLabel == null || contentTypeLabel.isBlank()
                ? "content"
                : contentTypeLabel;

        String safeTitle = title == null || title.isBlank()
                ? "New MEDIATRON update"
                : title.trim();

        if (safeTitle.length() > 90) {
            safeTitle = safeTitle.substring(0, 87) + "...";
        }

        return "New MEDIATRON " + safeType + " published: " + safeTitle;
    }

    private String buildContentPublishedBody(
            String contentTypeLabel,
            String title,
            String summary,
            String contentUrl,
            String unsubscribeUrl
    ) {
        String safeType = contentTypeLabel == null || contentTypeLabel.isBlank()
                ? "content"
                : contentTypeLabel;

        String safeTitle = title == null || title.isBlank()
                ? "New MEDIATRON update"
                : title.trim();

        String safeSummary = summary == null || summary.isBlank()
                ? "Details are available on the MEDIATRON portal."
                : summary.trim();

        String safeContentUrl = contentUrl == null || contentUrl.isBlank()
                ? loginUrl
                : contentUrl.trim();

        String safeUnsubscribeUrl = unsubscribeUrl == null || unsubscribeUrl.isBlank()
                ? ""
                : unsubscribeUrl.trim();

        String body = "Hello,\n\n"
                + "A new " + safeType + " has been published on the MEDIATRON portal.\n\n"
                + "Title:\n"
                + safeTitle + "\n\n"
                + "Details:\n"
                + safeSummary + "\n\n"
                + "Open it here:\n"
                + safeContentUrl + "\n\n"
                + "Regards,\n"
                + fromName + "\n\n";

        if (!safeUnsubscribeUrl.isBlank()) {
            body += "To stop receiving MEDIATRON updates, unsubscribe here:\n"
                    + safeUnsubscribeUrl + "\n";
        }

        return body;
    }
}