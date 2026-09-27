package com.Mediatron.Lab_Management.data_models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "members_publications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberPublication {

    @EmbeddedId
    private MemberPublicationId id = new MemberPublicationId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("memberId")
    @JoinColumn(name = "member_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("publicationId")
    @JoinColumn(name = "publication_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Publication publication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PublicationStatus status = PublicationStatus.PENDING;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (status == null) {
            status = PublicationStatus.PENDING;
        }
        verified = status == PublicationStatus.APPROVED;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        if (status == null) {
            status = PublicationStatus.PENDING;
        }
        verified = status == PublicationStatus.APPROVED;
        updatedAt = LocalDateTime.now();
    }
}
