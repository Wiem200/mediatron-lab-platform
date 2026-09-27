package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_models.Event;
import com.Mediatron.Lab_Management.data_models.EventStatus;
import com.Mediatron.Lab_Management.data_models.Project;
import com.Mediatron.Lab_Management.data_models.ProjectStatus;
import com.Mediatron.Lab_Management.data_models.Publication;
import com.Mediatron.Lab_Management.data_models.PublicationStatus;
import com.Mediatron.Lab_Management.data_models.Subscriber;
import com.Mediatron.Lab_Management.data_models.SubscriberContentNotification;
import com.Mediatron.Lab_Management.data_models.SubscriberContentType;
import com.Mediatron.Lab_Management.data_models.SubscriberStatus;
import com.Mediatron.Lab_Management.repositories.SubscriberContentNotificationRepository;
import com.Mediatron.Lab_Management.repositories.SubscriberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class SubscriberContentNotificationService {

    private final SubscriberRepository subscriberRepository;
    private final SubscriberContentNotificationRepository subscriberContentNotificationRepository;
    private final AccountEmailService accountEmailService;

    @Value("${app.frontend.public-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.backend.public-base-url:http://localhost:8080}")
    private String backendBaseUrl;

    public SubscriberContentNotificationService(
            SubscriberRepository subscriberRepository,
            SubscriberContentNotificationRepository subscriberContentNotificationRepository,
            AccountEmailService accountEmailService
    ) {
        this.subscriberRepository = subscriberRepository;
        this.subscriberContentNotificationRepository = subscriberContentNotificationRepository;
        this.accountEmailService = accountEmailService;
    }

    @Transactional
    public int notifyPublicationPublished(Publication publication) {
        if (publication == null
                || publication.getId() == null
                || publication.getStatus() != PublicationStatus.APPROVED) {
            return 0;
        }

        return notifyActiveSubscribers(
                SubscriberContentType.PUBLICATION,
                publication.getId(),
                cleanTitle(publication.getTitle(), "New publication"),
                buildPublicationSummary(publication),
                buildPublicationUrl(publication)
        );
    }

    @Transactional
    public int notifyProjectPublished(Project project) {
        if (project == null
                || project.getId() == null
                || project.getStatus() != ProjectStatus.APPROVED) {
            return 0;
        }

        return notifyActiveSubscribers(
                SubscriberContentType.PROJECT,
                project.getId(),
                cleanTitle(project.getTitle(), "New research project"),
                cleanSummary(project.getDescription()),
                buildProjectUrl(project)
        );
    }

    @Transactional
    public int notifyEventPublished(Event event) {
        if (event == null
                || event.getId() == null
                || event.getStatus() != EventStatus.APPROVED) {
            return 0;
        }

        return notifyActiveSubscribers(
                SubscriberContentType.EVENT,
                event.getId(),
                cleanTitle(event.getTitle(), "New event"),
                buildEventSummary(event),
                buildEventUrl(event)
        );
    }

    private int notifyActiveSubscribers(
            SubscriberContentType contentType,
            Integer contentId,
            String title,
            String summary,
            String contentUrl
    ) {
        List<Subscriber> activeSubscribers = subscriberRepository.findByStatus(SubscriberStatus.ACTIVE);
        int sentCount = 0;

        for (Subscriber subscriber : activeSubscribers) {
            if (subscriber.getId() == null || subscriber.getEmail() == null || subscriber.getEmail().isBlank()) {
                continue;
            }

            ensureSubscriberHasUnsubscribeToken(subscriber);

            boolean alreadySent = subscriberContentNotificationRepository
                    .existsBySubscriber_IdAndContentTypeAndContentId(
                            subscriber.getId(),
                            contentType,
                            contentId
                    );

            if (alreadySent) {
                continue;
            }

            try {
                SubscriberContentNotification notification = new SubscriberContentNotification(
                        subscriber,
                        contentType,
                        contentId,
                        subscriber.getEmail()
                );

                subscriberContentNotificationRepository.saveAndFlush(notification);

                accountEmailService.sendSubscriberContentPublishedEmail(
                        subscriber.getEmail(),
                        toContentLabel(contentType),
                        title,
                        summary,
                        contentUrl,
                        buildUnsubscribeUrl(subscriber)
                );

                sentCount++;
            } catch (DataIntegrityViolationException ignored) {
                // Another request already created the notification row.
                // Unique constraint prevents duplicate emails for the same subscriber/content.
            }
        }

        return sentCount;
    }

    private void ensureSubscriberHasUnsubscribeToken(Subscriber subscriber) {
        if (subscriber.getUnsubscribeToken() != null && !subscriber.getUnsubscribeToken().isBlank()) {
            return;
        }

        subscriber.setUnsubscribeToken(UUID.randomUUID().toString());
        subscriberRepository.saveAndFlush(subscriber);
    }

    private String buildPublicationSummary(Publication publication) {
        String authors = cleanNullable(publication.getAuthorsText());
        String year = publication.getPublicationYear() == null ? null : String.valueOf(publication.getPublicationYear());
        String doi = cleanNullable(publication.getDoi());

        StringBuilder summary = new StringBuilder();

        if (authors != null) {
            summary.append("Authors: ").append(authors).append("\n");
        }

        if (year != null) {
            summary.append("Year: ").append(year).append("\n");
        }

        if (doi != null) {
            summary.append("DOI: ").append(doi).append("\n");
        }

        return summary.toString().trim();
    }

    private String buildEventSummary(Event event) {
        StringBuilder summary = new StringBuilder();

        if (event.getEventDate() != null) {
            summary.append("Date: ")
                    .append(event.getEventDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .append("\n");
        }

        if (cleanNullable(event.getLocation()) != null) {
            summary.append("Location: ").append(event.getLocation().trim()).append("\n");
        }

        if (cleanNullable(event.getResponsiblePerson()) != null) {
            summary.append("Responsible person: ").append(event.getResponsiblePerson().trim()).append("\n");
        }

        if (cleanNullable(event.getDescription()) != null) {
            summary.append("\n").append(event.getDescription().trim());
        }

        return summary.toString().trim();
    }

    private String buildPublicationUrl(Publication publication) {
        String title = cleanNullable(publication.getTitle());

        if (title == null) {
            return trimTrailingSlash(frontendBaseUrl) + "/publications";
        }

        return trimTrailingSlash(frontendBaseUrl)
                + "/publications?search="
                + encode(title);
    }

    private String buildProjectUrl(Project project) {
        return trimTrailingSlash(frontendBaseUrl) + "/projects/" + project.getId();
    }

    private String buildEventUrl(Event event) {
        String title = cleanNullable(event.getTitle());

        if (title == null) {
            return trimTrailingSlash(frontendBaseUrl) + "/events";
        }

        return trimTrailingSlash(frontendBaseUrl)
                + "/events?search="
                + encode(title);
    }

    private String buildUnsubscribeUrl(Subscriber subscriber) {
        return trimTrailingSlash(backendBaseUrl)
                + "/api/public/unsubscribe?token="
                + encode(subscriber.getUnsubscribeToken());
    }

    private String toContentLabel(SubscriberContentType contentType) {
        return switch (contentType) {
            case PUBLICATION -> "publication";
            case PROJECT -> "research project";
            case EVENT -> "event";
        };
    }

    private String cleanTitle(String value, String fallback) {
        String cleaned = cleanNullable(value);
        return cleaned == null ? fallback : cleaned;
    }

    private String cleanSummary(String value) {
        String cleaned = cleanNullable(value);
        return cleaned == null ? "Details are available on the MEDIATRON portal." : cleaned;
    }

    private String cleanNullable(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        return cleaned.isBlank() ? null : cleaned;
    }

    private String trimTrailingSlash(String value) {
        String cleaned = value == null || value.isBlank()
                ? "http://localhost:5173"
                : value.trim();

        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        return cleaned;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .toLowerCase(Locale.ROOT);
    }
}