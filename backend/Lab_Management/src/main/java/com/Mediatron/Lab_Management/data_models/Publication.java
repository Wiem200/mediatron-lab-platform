package com.Mediatron.Lab_Management.data_models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(name = "publications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    private String doi;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "authors_text", columnDefinition = "TEXT")
    private String authorsText;

    @Column(columnDefinition = "TEXT")
    private String source;

    @Column(name = "external_url", columnDefinition = "TEXT")
    private String externalUrl;

    @Column(name = "pdf_url", columnDefinition = "TEXT")
    private String pdfUrl;

    @Column(columnDefinition = "TEXT")
    private String indexation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PublicationType type = PublicationType.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PublicationStatus status = PublicationStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnoreProperties({"publications", "publicationLinks", "projects", "supervisedStudents", "user"})
    @ManyToMany
    @JoinTable(
            name = "members_publications",
            joinColumns = @JoinColumn(name = "publication_id"),
            inverseJoinColumns = @JoinColumn(name = "member_id")
    )
    private Set<Member> members = new HashSet<>();

    @OneToMany(mappedBy = "publication")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<MemberPublication> memberLinks = new LinkedHashSet<>();

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (status == null) {
            status = PublicationStatus.PENDING;
        }
        if (type == null) {
            type = PublicationType.OTHER;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        if (status == null) {
            status = PublicationStatus.PENDING;
        }
        if (type == null) {
            type = PublicationType.OTHER;
        }
        updatedAt = LocalDateTime.now();
    }

    public void setTitle(String title) {
        this.title = cleanNullable(title);
    }

    public void setDoi(String doi) {
        this.doi = normalizeDoi(doi);
    }

    public void setAuthorsText(String authorsText) {
        this.authorsText = cleanNullable(authorsText);
    }

    public void setSource(String source) {
        this.source = cleanNullable(source);
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = cleanNullable(externalUrl);
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = cleanNullable(pdfUrl);
    }

    public void setIndexation(String indexation) {
        this.indexation = cleanNullable(indexation);
    }

    private String cleanNullable(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String normalizeDoi(String value) {
        String cleaned = cleanNullable(value);
        if (cleaned == null) {
            return null;
        }
        cleaned = cleaned
                .replace("https://doi.org/", "")
                .replace("http://doi.org/", "")
                .replace("https://dx.doi.org/", "")
                .replace("http://dx.doi.org/", "")
                .trim();
        return cleaned.isBlank() ? null : cleaned.toLowerCase(Locale.ROOT);
    }
}
