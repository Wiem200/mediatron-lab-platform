package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.data_dto.EventDTO;
import com.Mediatron.Lab_Management.data_dto.EventValidationRequest;
import com.Mediatron.Lab_Management.data_models.Event;
import com.Mediatron.Lab_Management.services.EventService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private ModelMapper modelMapper;

    @PostMapping("/add")
    @PreAuthorize("@delegationService.hasAdminPrivileges(authentication)")
    public ResponseEntity<EventDTO> addEvent(@Valid @RequestBody Event event) {
        Event savedEvent = eventService.create(event);
        return ResponseEntity.ok(toDto(savedEvent));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LAB_HEAD')")
    public List<EventDTO> getAllEvents() {
        return eventService.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/pending-validation")
    @PreAuthorize("hasRole('LAB_HEAD')")
    public List<EventDTO> getPendingValidationEvents() {
        return eventService.findPendingValidation().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}/validate")
    @PreAuthorize("hasRole('LAB_HEAD')")
    public ResponseEntity<EventDTO> validateEvent(
            @PathVariable Integer id,
            @Valid @RequestBody EventValidationRequest request
    ) {
        return ResponseEntity.ok(toDto(eventService.validate(id, request.getAction())));
    }

    @GetMapping("/active")
    public List<EventDTO> getPublicEvents() {
        return eventService.findApprovedPublicEvents().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/public")
    public List<EventDTO> getLegacyPublicEvents() {
        return getPublicEvents();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@delegationService.hasAdminPrivileges(authentication)")
    public ResponseEntity<?> deleteEvent(@PathVariable Integer id) {
        eventService.delete(id);
        return ResponseEntity.ok("Event deleted");
    }

    private EventDTO toDto(Event event) {
        return modelMapper.map(event, EventDTO.class);
    }
}
