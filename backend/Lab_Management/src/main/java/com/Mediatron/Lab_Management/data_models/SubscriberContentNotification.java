package com.Mediatron.Lab_Management.data_models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "subscriber_content_notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_subscriber_content_notification_once",
                columnNames = {"subscriber_id", "content_type", "content_id"}
        )
)
@Data
@NoArgsConstructor
public class SubscriberContentNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscriber_id", nullable = false)
    private Subscriber subscriber;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 30)
    private SubscriberContentType contentType;

    @Column(name = "content_id", nullable = false)
    private Integer contentId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    public SubscriberContentNotification(
            Subscriber subscriber,
            SubscriberContentType contentType,
            Integer contentId,
            String recipientEmail
    ) {
        this.subscriber = subscriber;
        this.contentType = contentType;
        this.contentId = contentId;
        this.recipientEmail = recipientEmail;
    }

    @PrePersist
    void onCreate() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }
}