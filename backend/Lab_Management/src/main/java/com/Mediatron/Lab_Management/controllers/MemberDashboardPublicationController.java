package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.data_dto.PublicationPublicDto;
import com.Mediatron.Lab_Management.data_dto.PublicationStatusUpdateRequest;
import com.Mediatron.Lab_Management.data_dto.PublicationValidationHistoryDto;
import com.Mediatron.Lab_Management.data_models.Publication;
import com.Mediatron.Lab_Management.data_models.PublicationStatus;
import com.Mediatron.Lab_Management.data_models.PublicationType;
import com.Mediatron.Lab_Management.repositories.PublicationRepository;
import com.Mediatron.Lab_Management.services.PublicationValidationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member/publications")
@CrossOrigin(origins = "*")
public class MemberDashboardPublicationController {

    private final PublicationRepository publicationRepository;
    private final PublicationValidationService publicationValidationService;

    public MemberDashboardPublicationController(
            PublicationRepository publicationRepository,
            PublicationValidationService publicationValidationService
    ) {
        this.publicationRepository = publicationRepository;
        this.publicationValidationService = publicationValidationService;
    }

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('MEMBER','ADMIN','LAB_HEAD')")
    public ResponseEntity<Page<PublicationPublicDto>> getMemberDashboardPublications(
            @PathVariable Integer memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PublicationType type,
            @RequestParam(required = false) PublicationStatus status,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            Authentication authentication
    ) {
        publicationValidationService.assertCanAccessMemberDashboard(memberId, authentication);

        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);

        Page<PublicationPublicDto> result = publicationRepository.searchDashboardPublicationsForMember(
                memberId,
                normalizeSearch(search),
                year,
                type,
                status,
                pageable
        ).map(this::toPublicDto);

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/member/{memberId}/{publicationId}/status")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<PublicationPublicDto> updateMemberPublicationStatus(
            @PathVariable Integer memberId,
            @PathVariable Integer publicationId,
            @RequestBody PublicationStatusUpdateRequest request,
            Authentication authentication
    ) {
        PublicationPublicDto updated = publicationValidationService.memberValidatePublication(
                memberId,
                publicationId,
                request,
                authentication
        );

        return ResponseEntity.ok(updated);
    }

    @GetMapping("/member/{memberId}/{publicationId}/history")
    @PreAuthorize("hasAnyRole('MEMBER','ADMIN','LAB_HEAD')")
    public ResponseEntity<List<PublicationValidationHistoryDto>> getMemberPublicationHistory(
            @PathVariable Integer memberId,
            @PathVariable Integer publicationId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                publicationValidationService.getMemberPublicationHistory(
                        memberId,
                        publicationId,
                        authentication
                )
        );
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

    private Pageable buildPageable(int page, int size, String sortBy, String sortDirection) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);

        return PageRequest.of(safePage, safeSize, buildPublicationSort(sortBy, sortDirection));
    }

    private Sort buildPublicationSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "asc".equalsIgnoreCase(normalizeSearch(sortDirection))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        String normalizedSortBy = normalizeSearch(sortBy).toLowerCase();

        Sort.Order primaryOrder;

        switch (normalizedSortBy) {
            case "title":
                primaryOrder = new Sort.Order(direction, "title").ignoreCase().nullsLast();
                break;

            case "year":
            case "publicationyear":
            case "publication_year":
                primaryOrder = new Sort.Order(direction, "publicationYear").nullsLast();
                break;

            default:
                primaryOrder = new Sort.Order(Sort.Direction.DESC, "publicationYear").nullsLast();
                break;
        }

        return Sort.by(primaryOrder)
                .and(Sort.by(new Sort.Order(Sort.Direction.DESC, "createdAt").nullsLast()));
    }

    private String normalizeSearch(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();
        return cleaned.isBlank() ? "" : cleaned;
    }
}