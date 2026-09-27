package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_models.Member;
import com.Mediatron.Lab_Management.data_models.MemberPublication;
import com.Mediatron.Lab_Management.data_models.MemberPublicationNotification;
import com.Mediatron.Lab_Management.data_models.Publication;
import com.Mediatron.Lab_Management.data_models.PublicationStatus;
import com.Mediatron.Lab_Management.repositories.MemberPublicationNotificationRepository;
import com.Mediatron.Lab_Management.repositories.MemberPublicationRepository;
import com.Mediatron.Lab_Management.repositories.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class MemberPublicationNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MemberPublicationNotificationService.class);

    private final MemberRepository memberRepository;
    private final MemberPublicationRepository memberPublicationRepository;
    private final MemberPublicationNotificationRepository notificationRepository;
    private final AccountEmailService accountEmailService;

    public MemberPublicationNotificationService(
            MemberRepository memberRepository,
            MemberPublicationRepository memberPublicationRepository,
            MemberPublicationNotificationRepository notificationRepository,
            AccountEmailService accountEmailService
    ) {
        this.memberRepository = memberRepository;
        this.memberPublicationRepository = memberPublicationRepository;
        this.notificationRepository = notificationRepository;
        this.accountEmailService = accountEmailService;
    }

    @Transactional
    public int notifyPendingPublicationsForMember(Integer memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("Member not found with id: " + memberId));

        String recipientEmail = cleanText(member.resolveEmail());

        if (recipientEmail == null) {
            log.warn("Skipping publication notification for member {} because no email is available.", memberId);
            return 0;
        }

        List<MemberPublication> pendingPublications =
                memberPublicationRepository.findAllForMemberByStatus(memberId, PublicationStatus.PENDING);

        int sentCount = 0;

        for (MemberPublication memberPublication : pendingPublications) {
            Publication publication = memberPublication.getPublication();

            if (publication == null || publication.getId() == null) {
                continue;
            }

            boolean alreadySent = notificationRepository.existsByMember_IdAndPublication_IdAndNotificationType(
                    member.getId(),
                    publication.getId(),
                    MemberPublicationNotification.TYPE_PUBLICATION_DETECTED
            );

            if (alreadySent) {
                continue;
            }

            try {
                MemberPublicationNotification notification = new MemberPublicationNotification();
                notification.setMember(member);
                notification.setPublication(publication);
                notification.setNotificationType(MemberPublicationNotification.TYPE_PUBLICATION_DETECTED);
                notification.setRecipientEmail(recipientEmail);

                notificationRepository.saveAndFlush(notification);

                accountEmailService.sendPublicationDetectedEmail(
                        recipientEmail,
                        buildMemberName(member),
                        publication.getTitle(),
                        publication.getAuthorsText(),
                        buildSourceLink(publication)
                );

                sentCount++;
            } catch (DataIntegrityViolationException ex) {
                log.info(
                        "Publication notification already exists for member {} and publication {}.",
                        member.getId(),
                        publication.getId()
                );
            }
        }

        return sentCount;
    }

    private String buildSourceLink(Publication publication) {
        String externalUrl = cleanText(publication.getExternalUrl());

        if (externalUrl != null) {
            return externalUrl;
        }

        String doi = cleanText(publication.getDoi());

        if (doi == null) {
            return null;
        }

        if (doi.startsWith("http://") || doi.startsWith("https://")) {
            return doi;
        }

        return "https://doi.org/" + doi;
    }

    private String buildMemberName(Member member) {
        String firstName = member.getFirstName() == null ? "" : member.getFirstName().trim();
        String lastName = member.getLastName() == null ? "" : member.getLastName().trim();

        String fullName = (firstName + " " + lastName).trim();

        return fullName.isBlank() ? "there" : fullName;
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        return cleaned.isBlank() ? null : cleaned;
    }
}