package com.Mediatron.Lab_Management.data_models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"responsibleMember", "members", "phdStudents"})
@EqualsAndHashCode(exclude = {"responsibleMember", "members", "phdStudents"})
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String title;

    @NotBlank
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false)
    private String duration;

    @Column(columnDefinition = "TEXT")
    private String funding;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "github_link", columnDefinition = "TEXT")
    private String githubLink;

    @Column(name = "video_link", columnDefinition = "TEXT")
    private String videoLink;

    @Column(name = "pdf_link", columnDefinition = "TEXT")
    private String pdfLink;

    @Column(name = "poster_link", columnDefinition = "TEXT")
    private String posterLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProjectStatus status = ProjectStatus.PENDING_HEAD_VALIDATION;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_member_id")
    @JsonIgnoreProperties({"projects", "supervisedStudents", "user"})
    private Member responsibleMember;

    @ManyToMany(mappedBy = "projects")
    @JsonIgnoreProperties({"projects", "supervisedStudents", "user"})
    private Set<Member> members = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "project_phds",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "phd_student_id")
    )
    @JsonIgnoreProperties({"supervisor"})
    private Set<PhDStudent> phdStudents = new HashSet<>();
}
