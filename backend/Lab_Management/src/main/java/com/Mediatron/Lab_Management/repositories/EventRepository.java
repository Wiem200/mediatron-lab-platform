package com.Mediatron.Lab_Management.repositories;

import com.Mediatron.Lab_Management.data_models.Event;
import com.Mediatron.Lab_Management.data_models.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);
}
