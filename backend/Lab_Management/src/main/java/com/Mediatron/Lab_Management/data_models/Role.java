package com.Mediatron.Lab_Management.data_models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)// Indispensable pour stocker le texte (ADMIN, MEMBER...) au lieu d'un chiffre


    @Column(length = 20, unique = true , nullable = false)
    private ERole name;
}
