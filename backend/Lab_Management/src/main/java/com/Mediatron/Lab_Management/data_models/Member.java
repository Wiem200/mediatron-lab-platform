package com.Mediatron.Lab_Management.data_models;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "members")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String grade;

    private String lab = "MEDIATRON";
    private String orcidId;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(columnDefinition = "TEXT")
    private String photoUrl;

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String linkedinUrl;

    @Column(columnDefinition = "TEXT")
    private String googleScholarUrl;

    @Column(columnDefinition = "TEXT")
    private String personalSiteUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_research_axes", joinColumns = @JoinColumn(name = "member_id"))
    @Column(name = "research_axes")
    private Set<String> researchAxes = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "project_members",
            joinColumns = @JoinColumn(name = "member_id"),
            inverseJoinColumns = @JoinColumn(name = "project_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Project> projects = new HashSet<>();

    @OneToMany(mappedBy = "supervisor")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<PhDStudent> supervisedStudents = new HashSet<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnoreProperties({"members", "memberLinks"})
    @ManyToMany
    @JoinTable(
            name = "members_publications",
            joinColumns = @JoinColumn(name = "member_id"),
            inverseJoinColumns = @JoinColumn(name = "publication_id")
    )
    private Set<Publication> publications = new LinkedHashSet<>();

    @OneToMany(mappedBy = "member")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<MemberPublication> publicationLinks = new LinkedHashSet<>();

    @Transient
    private String email;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (lab == null || lab.isBlank()) {
            lab = "MEDIATRON";
        }
        if (grade == null || grade.isBlank()) {
            grade = "Member";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (lab == null || lab.isBlank()) {
            lab = "MEDIATRON";
        }
    }

    public String getPosition() {
        return grade;
    }

    public void setPosition(String position) {
        this.grade = position;
    }

    public String getPersonalWebsite() {
        return personalSiteUrl;
    }

    public void setPersonalWebsite(String personalWebsite) {
        this.personalSiteUrl = personalWebsite;
    }

    public String resolveEmail() {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return user != null ? user.getEmail() : null;
    }
}
