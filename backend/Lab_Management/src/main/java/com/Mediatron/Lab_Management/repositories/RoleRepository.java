package com.Mediatron.Lab_Management.repositories;


import com.Mediatron.Lab_Management.data_models.ERole;
import com.Mediatron.Lab_Management.data_models.Role;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role , Integer> {
    // You MUST add this line manually!
    Optional<Role> findByName(ERole name);
}