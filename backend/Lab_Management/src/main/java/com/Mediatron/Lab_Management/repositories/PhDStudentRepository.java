package com.Mediatron.Lab_Management.repositories;

import com.Mediatron.Lab_Management.data_models.PhDStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhDStudentRepository extends JpaRepository<PhDStudent, Integer> {
    List<PhDStudent> findAllByOrderByRegistrationYearDescLastNameAscFirstNameAsc();

    List<PhDStudent> findBySupervisorId(Integer memberId);
}
