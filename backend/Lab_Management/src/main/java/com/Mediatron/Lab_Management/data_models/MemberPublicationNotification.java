package com.Mediatron.Lab_Management.data_models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "member_publication_notifications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_member_publication_notification",
                        columnNames = {"member_id", "publication_id", "notification_type"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberPublicationNotification {

    public static final String TYPE_PUBLICATION_DETECTED = "PUBLICATION_DETECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Publication publication;

    @Column(name = "notification_type", nullable = false, length = 80)
    private String notificationType = TYPE_PUBLICATION_DETECTED;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (sentAt == null) {
            sentAt = now;
        }

        if (notificationType == null || notificationType.isBlank()) {
            notificationType = TYPE_PUBLICATION_DETECTED;
        }
    }
}