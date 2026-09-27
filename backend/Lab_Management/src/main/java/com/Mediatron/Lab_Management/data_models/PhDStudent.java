package com.Mediatron.Lab_Management.data_models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "phd_students")
@ToString(exclude = {"supervisor", "publications"})
@EqualsAndHashCode(exclude = {"supervisor", "publications"})
public class PhDStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    @NotBlank
    @Column(name = "thesis_topic", columnDefinition = "TEXT")
    private String thesisTopic;

    @Min(2000)
    @Column(name = "registration_year")
    private Integer registrationYear;

    @NotBlank
    @Column(name = "research_team", length = 150)
    private String researchTeam;

    @Column(name = "co_supervisor", length = 150)
    private String coSupervisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    @JsonIgnoreProperties({"projects", "supervisedStudents", "user"})
    private Member supervisor;

    @ManyToMany
    @JoinTable(
            name = "phd_student_publications",
            joinColumns = @JoinColumn(name = "phd_student_id"),
            inverseJoinColumns = @JoinColumn(name = "publication_id")
    )
    private Set<Publication> publications = new LinkedHashSet<>();
}
