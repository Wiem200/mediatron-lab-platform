package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_dto.MemberPublicationDTO;
import com.Mediatron.Lab_Management.data_dto.PublicationPublicDto;
import com.Mediatron.Lab_Management.data_dto.PublicationStatusUpdateRequest;
import com.Mediatron.Lab_Management.data_dto.PublicationValidationHistoryDto;
import com.Mediatron.Lab_Management.data_dto.PublicationValidationQueueDto;
import com.Mediatron.Lab_Management.data_models.Member;
import com.Mediatron.Lab_Management.data_models.MemberPublication;
import com.Mediatron.Lab_Management.data_models.Publication;
import com.Mediatron.Lab_Management.data_models.PublicationStatus;
import com.Mediatron.Lab_Management.data_models.PublicationType;
import com.Mediatron.Lab_Management.data_models.PublicationValidationHistory;
import com.Mediatron.Lab_Management.repositories.MemberPublicationRepository;
import com.Mediatron.Lab_Management.repositories.MemberRepository;
import com.Mediatron.Lab_Management.repositories.PublicationRepository;
import com.Mediatron.Lab_Management.repositories.PublicationValidationHistoryRepository;
import com.Mediatron.Lab_Management.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class PublicationValidationService {

    private final PublicationRepository publicationRepository;
    private final PublicationValidationHistoryRepository publicationValidationHistoryRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final MemberPublicationRepository memberPublicationRepository;
    private final SubscriberContentNotificationService subscriberContentNotificationService;

    public PublicationValidationService(
            PublicationRepository publicationRepository,
            PublicationValidationHistoryRepository publicationValidationHistoryRepository,
            MemberRepository memberRepository,
            UserRepository userRepository,
            MemberPublicationRepository memberPublicationRepository,
            SubscriberContentNotificationService subscriberContentNotificationService
    ) {
        this.publicationRepository = publicationRepository;
        this.publicationValidationHistoryRepository = publicationValidationHistoryRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.memberPublicationRepository = memberPublicationRepository;
        this.subscriberContentNotificationService = subscriberContentNotificationService;
    }

    public void assertCanAccessMemberDashboard(Integer memberId, Authentication authentication) {
        if (isPrivileged(authentication)) {
            return;
        }

        Member currentMember = getCurrentMember(authentication);

        if (!currentMember.getId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Members can only access their own publication dashboard.");
        }
    }

    public PublicationPublicDto memberValidatePublication(
            Integer memberId,
            Integer publicationId,
            PublicationStatusUpdateRequest request,
            Authentication authentication
    ) {
        validateRequest(request);

        if (request.getStatus() != PublicationStatus.APPROVED && request.getStatus() != PublicationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Members can only set publication status to APPROVED or REJECTED.");
        }

        Member currentMember = getCurrentMember(authentication);

        if (!currentMember.getId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Members can only validate their own publications.");
        }

        Publication publication = publicationRepository.findByIdAndMembers_Id(publicationId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publication not found for this member."));

        PublicationStatus previousStatus = publication.getStatus();

        Publication saved = applyStatusUpdate(publication, request, authentication, "MEMBER_VALIDATION");

        notifySubscribersIfPublicationPublished(previousStatus, saved);

        return toPublicDto(saved);
    }

    public Page<PublicationValidationQueueDto> getValidationQueue(
            Integer memberId,
            String search,
            Integer year,
            PublicationType type,
            PublicationStatus status,
            Pageable pageable
    ) {
        return publicationRepository
                .searchValidationQueue(memberId, normalizeSearch(search), year, type, status, pageable)
                .map(this::toValidationQueueDto);
    }

    public PublicationValidationQueueDto reviewerValidatePublication(
            Integer publicationId,
            PublicationStatusUpdateRequest request,
            Authentication authentication
    ) {
        validateRequest(request);

        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publication not found."));

        PublicationStatus previousStatus = publication.getStatus();

        String actionType = hasRole(authentication, "LAB_HEAD") ? "LAB_HEAD_VALIDATION" : "ADMIN_VALIDATION";

        Publication saved = applyStatusUpdate(publication, request, authentication, actionType);

        notifySubscribersIfPublicationPublished(previousStatus, saved);

        return toValidationQueueDto(saved);
    }

    public List<PublicationValidationHistoryDto> getMemberPublicationHistory(
            Integer memberId,
            Integer publicationId,
            Authentication authentication
    ) {
        assertCanAccessMemberDashboard(memberId, authentication);

        publicationRepository.findByIdAndMembers_Id(publicationId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publication not found for this member."));

        return publicationValidationHistoryRepository
                .findByPublication_IdOrderByCreatedAtDescIdDesc(publicationId)
                .stream()
                .map(this::toHistoryDto)
                .toList();
    }

    public List<PublicationValidationHistoryDto> getReviewerPublicationHistory(Integer publicationId) {
        if (!publicationRepository.existsById(publicationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Publication not found.");
        }

        return publicationValidationHistoryRepository
                .findByPublication_IdOrderByCreatedAtDescIdDesc(publicationId)
                .stream()
                .map(this::toHistoryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberPublicationDTO> findPublicationsForAuthenticatedMember(Integer userId) {
        Member member = getRequiredMemberByUserId(userId);

        return memberPublicationRepository
                .findAllForMember(member.getId())
                .stream()
                .map(this::toMemberPublicationDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberPublicationDTO> findPendingPublicationsForAuthenticatedMember(Integer userId) {
        return findPublicationsForAuthenticatedMember(userId)
                .stream()
                .filter(item -> PublicationStatus.PENDING.name().equals(item.getStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberPublicationDTO> findApprovedPublicationsForMember(Integer memberId) {
        return memberPublicationRepository
                .findAllForMemberByStatus(memberId, PublicationStatus.APPROVED)
                .stream()
                .map(this::toMemberPublicationDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberPublicationDTO> findAllLinkedPublicationsForMember(Integer memberId) {
        return memberPublicationRepository
                .findAllForMember(memberId)
                .stream()
                .map(this::toMemberPublicationDto)
                .toList();
    }

    @Transactional
    public MemberPublicationDTO approvePublication(Integer userId, Integer publicationId) {
        return updatePublicationStatus(userId, publicationId, PublicationStatus.APPROVED);
    }

    @Transactional
    public MemberPublicationDTO rejectPublication(Integer userId, Integer publicationId) {
        return updatePublicationStatus(userId, publicationId, PublicationStatus.REJECTED);
    }

    @Transactional
    public MemberPublicationDTO validatePublication(Integer userId, Integer publicationId, String action) {
        String normalizedAction = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);

        return switch (normalizedAction) {
            case "APPROVE", "APPROVED" -> approvePublication(userId, publicationId);
            case "REJECT", "REJECTED" -> rejectPublication(userId, publicationId);
            default -> throw new IllegalArgumentException("Action must be APPROVE or REJECT.");
        };
    }

    private MemberPublicationDTO updatePublicationStatus(
            Integer userId,
            Integer publicationId,
            PublicationStatus nextStatus
    ) {
        Member member = getRequiredMemberByUserId(userId);

        MemberPublication memberPublication = memberPublicationRepository
                .findForMemberAndPublication(member.getId(), publicationId)
                .orElseThrow(() -> new NoSuchElementException("Publication not found for this member."));

        PublicationStatus previousMemberStatus = memberPublication.getStatus();

        if (previousMemberStatus == nextStatus) {
            return toMemberPublicationDto(memberPublication);
        }

        Publication publication = memberPublication.getPublication();
        PublicationStatus previousAggregateStatus = publication.getStatus();

        memberPublication.setStatus(nextStatus);
        memberPublication.setVerified(nextStatus == PublicationStatus.APPROVED);

        MemberPublication saved = memberPublicationRepository.save(memberPublication);

        Publication aggregatePublication = syncAggregatePublicationStatus(publication);

        notifySubscribersIfPublicationPublished(previousAggregateStatus, aggregatePublication);

        return toMemberPublicationDto(saved);
    }

    private Publication syncAggregatePublicationStatus(Publication publication) {
        List<PublicationStatus> statuses = memberPublicationRepository.findStatusesForPublication(publication.getId());

        PublicationStatus aggregateStatus = PublicationStatus.PENDING;

        if (statuses.stream().anyMatch(status -> status == PublicationStatus.APPROVED)) {
            aggregateStatus = PublicationStatus.APPROVED;
        } else if (!statuses.isEmpty() && statuses.stream().allMatch(status -> status == PublicationStatus.REJECTED)) {
            aggregateStatus = PublicationStatus.REJECTED;
        }

        publication.setStatus(aggregateStatus);
        return publicationRepository.save(publication);
    }

    private void notifySubscribersIfPublicationPublished(
            PublicationStatus previousStatus,
            Publication publication
    ) {
        if (publication == null || publication.getStatus() != PublicationStatus.APPROVED) {
            return;
        }

        if (previousStatus == PublicationStatus.APPROVED) {
            return;
        }

        try {
            int sentCount = subscriberContentNotificationService.notifyPublicationPublished(publication);

            if (sentCount > 0) {
                System.out.println(
                        "Subscriber publication notification emails sent for publication "
                                + publication.getId()
                                + ": "
                                + sentCount
                );
            }
        } catch (Exception error) {
            System.err.println(
                    "Subscriber publication notification failed for publication "
                            + publication.getId()
                            + ": "
                            + cleanErrorMessage(error)
            );
        }
    }

    private Member getRequiredMemberByUserId(Integer userId) {
        return memberRepository
                .findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("No member profile is linked to the authenticated user."));
    }

    private Publication applyStatusUpdate(
            Publication publication,
            PublicationStatusUpdateRequest request,
            Authentication authentication,
            String actionType
    ) {
        PublicationStatus previousStatus = publication.getStatus();

        publication.setStatus(request.getStatus());

        Publication saved = publicationRepository.save(publication);

        recordHistory(saved, authentication, actionType, previousStatus, request.getStatus(), request.getNote());

        return saved;
    }

    private void recordHistory(
            Publication publication,
            Authentication authentication,
            String actionType,
            PublicationStatus previousStatus,
            PublicationStatus newStatus,
            String note
    ) {
        PublicationValidationHistory history = new PublicationValidationHistory();

        history.setPublication(publication);
        history.setActorEmail(getActorEmail(authentication));
        history.setActorRole(resolveActorRole(authentication));
        history.setActionType(actionType);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setNote(cleanNullable(note));

        userRepository.findByEmail(getActorEmail(authentication)).ifPresent(history::setActorUser);

        publicationValidationHistoryRepository.save(history);
    }

    private void validateRequest(PublicationStatusUpdateRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
    }

    private Member getCurrentMember(Authentication authentication) {
        return memberRepository
                .findByUserEmail(getActorEmail(authentication))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated member profile not found."));
    }

    private boolean isPrivileged(Authentication authentication) {
        return hasRole(authentication, "ADMIN") || hasRole(authentication, "LAB_HEAD");
    }

    private boolean hasRole(Authentication authentication, String roleName) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        String expectedAuthority = "ROLE_" + roleName;

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (expectedAuthority.equals(authority.getAuthority())) {
                return true;
            }
        }

        return false;
    }

    private String getActorEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found.");
        }

        return authentication.getName().trim();
    }

    private String resolveActorRole(Authentication authentication) {
        if (hasRole(authentication, "LAB_HEAD")) {
            return "LAB_HEAD";
        }

        if (hasRole(authentication, "ADMIN")) {
            return "ADMIN";
        }

        if (hasRole(authentication, "MEMBER")) {
            return "MEMBER";
        }

        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(value -> value != null && !value.isBlank())
                    .map(value -> value.startsWith("ROLE_") ? value.substring(5) : value)
                    .findFirst()
                    .orElse("UNKNOWN");
        }

        return "UNKNOWN";
    }

    private PublicationPublicDto toPublicDto(Publication publication) {
        return new PublicationPublicDto(
                publication.getId(),
                publication.getTitle(),
                publication.getDoi(),
                publication.getPublicationYear(),
                publication.getAuthorsText(),
                publication.getSource(),
                publication.getExternalUrl(),
                publication.getPdfUrl(),
                publication.getIndexation(),
                publication.getType(),
                publication.getStatus()
        );
    }

    private PublicationValidationQueueDto toValidationQueueDto(Publication publication) {
        List<String> memberNames = publication
                .getMembers()
                .stream()
                .map(this::buildMemberName)
                .filter(name -> !name.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        return new PublicationValidationQueueDto(
                publication.getId(),
                publication.getTitle(),
                publication.getDoi(),
                publication.getPublicationYear(),
                publication.getAuthorsText(),
                publication.getSource(),
                publication.getExternalUrl(),
                publication.getPdfUrl(),
                publication.getIndexation(),
                publication.getType(),
                publication.getStatus(),
                memberNames
        );
    }

    private MemberPublicationDTO toMemberPublicationDto(MemberPublication memberPublication) {
        Publication publication = memberPublication.getPublication();
        PublicationType type = publication.getType();

        return new MemberPublicationDTO(
                publication.getId(),
                publication.getSource(),
                publication.getTitle(),
                publication.getAuthorsText(),
                publication.getPublicationYear(),
                publication.getDoi(),
                publication.getExternalUrl(),
                type == null ? null : type.name(),
                memberPublication.getStatus().name()
        );
    }

    private PublicationValidationHistoryDto toHistoryDto(PublicationValidationHistory history) {
        return new PublicationValidationHistoryDto(
                history.getId(),
                history.getActorEmail(),
                history.getActorRole(),
                history.getActionType(),
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getNote(),
                history.getCreatedAt()
        );
    }

    private String buildMemberName(Member member) {
        String firstName = member.getFirstName() == null ? "" : member.getFirstName().trim();
        String lastName = member.getLastName() == null ? "" : member.getLastName().trim();

        return (firstName + " " + lastName).trim();
    }

    private String normalizeSearch(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();

        return cleaned.isBlank() ? "" : cleaned;
    }

    private String cleanNullable(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        return cleaned.isBlank() ? null : cleaned;
    }

    private String cleanErrorMessage(Exception ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "Unknown error";
        }

        return ex.getMessage().trim();
    }
}