package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_dto.SubscriberDTO;
import com.Mediatron.Lab_Management.data_dto.SubscriberManagementDTO;
import com.Mediatron.Lab_Management.data_models.Subscriber;
import com.Mediatron.Lab_Management.data_models.SubscriberStatus;
import com.Mediatron.Lab_Management.repositories.SubscriberRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;
    private final AccountEmailService accountEmailService;

    public SubscriberService(SubscriberRepository subscriberRepository, AccountEmailService accountEmailService) {
        this.subscriberRepository = subscriberRepository;
        this.accountEmailService = accountEmailService;
    }

    @Transactional
    public boolean subscribe(SubscriberDTO dto) {
        String normalizedEmail = normalizeEmail(dto.getEmail());

        Subscriber existingSubscriber = subscriberRepository.findByEmail(normalizedEmail).orElse(null);

        if (existingSubscriber != null) {
            if (existingSubscriber.getStatus() == SubscriberStatus.ACTIVE) {
                return false;
            }

            existingSubscriber.setStatus(SubscriberStatus.ACTIVE);
            subscriberRepository.save(existingSubscriber);
            accountEmailService.sendSubscriberWelcomeEmail(normalizedEmail);
            return true;
        }

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(normalizedEmail);
        subscriber.setStatus(SubscriberStatus.ACTIVE);

        try {
            subscriberRepository.save(subscriber);
        } catch (DataIntegrityViolationException ex) {
            if (subscriberRepository.existsByEmail(normalizedEmail)) {
                return false;
            }
            throw ex;
        }

        accountEmailService.sendSubscriberWelcomeEmail(normalizedEmail);
        return true;
    }

    @Transactional
    public boolean unsubscribe(String token) {
        String cleanedToken = token == null ? "" : token.trim();

        if (cleanedToken.isBlank()) {
            return false;
        }

        Subscriber subscriber = subscriberRepository.findByUnsubscribeToken(cleanedToken).orElse(null);

        if (subscriber == null) {
            return false;
        }

        subscriber.setStatus(SubscriberStatus.INACTIVE);
        subscriberRepository.save(subscriber);

        return true;
    }

    @Transactional(readOnly = true)
    public List<SubscriberManagementDTO> findAll() {
        return subscriberRepository.findAll().stream()
                .sorted(Comparator.comparing(Subscriber::getSubscribedAt).reversed())
                .map(this::toManagementDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriberManagementDTO> searchByEmail(String email) {
        String normalizedQuery = normalizeEmail(email);
        if (normalizedQuery.isEmpty()) {
            return findAll();
        }

        return subscriberRepository.findByEmailContainingIgnoreCase(normalizedQuery).stream()
                .sorted(Comparator.comparing(Subscriber::getSubscribedAt).reversed())
                .map(this::toManagementDto)
                .toList();
    }

    @Transactional
    public SubscriberManagementDTO updateStatus(Integer id, SubscriberStatus status) {
        Subscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Subscriber not found."));

        subscriber.setStatus(status);
        return toManagementDto(subscriberRepository.save(subscriber));
    }

    @Transactional
    public void delete(Integer id) {
        Subscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Subscriber not found."));

        subscriberRepository.delete(subscriber);
    }

    private SubscriberManagementDTO toManagementDto(Subscriber subscriber) {
        return new SubscriberManagementDTO(
                subscriber.getId(),
                subscriber.getEmail(),
                subscriber.getStatus(),
                subscriber.getSubscribedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}