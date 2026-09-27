package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_models.Event;
import com.Mediatron.Lab_Management.data_models.EventStatus;
import com.Mediatron.Lab_Management.repositories.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final SubscriberContentNotificationService subscriberContentNotificationService;

    public EventService(
            EventRepository eventRepository,
            SubscriberContentNotificationService subscriberContentNotificationService
    ) {
        this.eventRepository = eventRepository;
        this.subscriberContentNotificationService = subscriberContentNotificationService;
    }

    public Event create(Event event) {
        event.setId(null);
        event.setStatus(EventStatus.PENDING_HEAD_VALIDATION);
        return eventRepository.save(event);
    }

    public List<Event> findAll() {
        return eventRepository.findAll();
    }

    public List<Event> findPendingValidation() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PENDING_HEAD_VALIDATION);
    }

    public List<Event> findApprovedPublicEvents() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.APPROVED);
    }

    public Event findById(Integer id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Event not found: " + id));
    }

    public Event update(Integer id, Event details) {
        Event event = findById(id);
        event.setTitle(details.getTitle());
        event.setDescription(details.getDescription());
        event.setEventDate(details.getEventDate());
        event.setLocation(details.getLocation());
        event.setResponsiblePerson(details.getResponsiblePerson());
        return eventRepository.save(event);
    }

    public void delete(Integer id) {
        Event event = findById(id);
        eventRepository.delete(event);
    }

    @Transactional
    public Event validate(Integer id, String action) {
        Event event = findById(id);

        if (event.getStatus() != EventStatus.PENDING_HEAD_VALIDATION) {
            throw new IllegalArgumentException("Only pending events can be validated.");
        }

        String normalizedAction = action == null ? "" : action.trim().toUpperCase();
        EventStatus nextStatus = switch (normalizedAction) {
            case "APPROVE" -> EventStatus.APPROVED;
            case "REJECT" -> EventStatus.REJECTED;
            default -> throw new IllegalArgumentException("Action must be APPROVE or REJECT.");
        };

        if (nextStatus == EventStatus.APPROVED) {
            ensureApprovalFields(event);
        }

        event.setStatus(nextStatus);
        Event savedEvent = eventRepository.save(event);

        if (savedEvent.getStatus() == EventStatus.APPROVED) {
            subscriberContentNotificationService.notifyEventPublished(savedEvent);
        }

        return savedEvent;
    }

    private void ensureApprovalFields(Event event) {
        if (event.getEventDate() == null) {
            throw new IllegalArgumentException("Event date is required before approval.");
        }
        if (event.getDescription() == null || event.getDescription().isBlank()) {
            throw new IllegalArgumentException("Description is required before approval.");
        }
        if (event.getResponsiblePerson() == null || event.getResponsiblePerson().isBlank()) {
            throw new IllegalArgumentException("Responsible person is required before approval.");
        }
    }
}