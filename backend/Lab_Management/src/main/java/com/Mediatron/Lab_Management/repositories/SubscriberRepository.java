package com.Mediatron.Lab_Management.repositories;

import com.Mediatron.Lab_Management.data_models.Subscriber;
import com.Mediatron.Lab_Management.data_models.SubscriberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Integer> {
    boolean existsByEmail(String email);

    Optional<Subscriber> findByEmail(String email);

    Optional<Subscriber> findByUnsubscribeToken(String unsubscribeToken);

    List<Subscriber> findByEmailContainingIgnoreCase(String email);

    List<Subscriber> findByStatus(SubscriberStatus status);
}