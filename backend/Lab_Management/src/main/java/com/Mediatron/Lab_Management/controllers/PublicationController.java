package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.authentication.UserPrincipal;
import com.Mediatron.Lab_Management.data_dto.MemberPublicationDTO;
import com.Mediatron.Lab_Management.data_dto.MemberPublicationValidationRequest;
import com.Mediatron.Lab_Management.data_dto.PublicationVisitorDto;
import com.Mediatron.Lab_Management.data_models.Publication;
import com.Mediatron.Lab_Management.data_models.PublicationStatus;
import com.Mediatron.Lab_Management.data_models.PublicationType;
import com.Mediatron.Lab_Management.repositories.PublicationRepository;
import com.Mediatron.Lab_Management.services.PublicationValidationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/publications")
@CrossOrigin(origins = "*")
public class PublicationController {

    private final PublicationRepository publicationRepository;
    private final PublicationValidationService publicationValidationService;

    public PublicationController(
            PublicationRepository publicationRepository,
            PublicationValidationService publicationValidationService
    ) {
        this.publicationRepository = publicationRepository;
        this.publicationValidationService = publicationValidationService;
    }

    @GetMapping
    public Page<PublicationVisitorDto> getPublications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PublicationType type,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);

        String effectiveSearch = normalizeSearch(search);
        if (effectiveSearch.isBlank()) {
            effectiveSearch = normalizeSearch(title);
        }

        Page<Publication> publications = publicationRepository.searchPublications(
                effectiveSearch,
                normalizeSearch(author),
                year,
                type,
                PublicationStatus.APPROVED,
                pageable
        );

        return publications.map(this::toVisitorDto);
    }

    @GetMapping("/types")
    public PublicationType[] getPublicationTypes() {
        return PublicationType.values();
    }

    @GetMapping("/years")
    public List<Integer> getPublicationYears() {
        return publicationRepository.findDistinctPublicationYearsByStatus(PublicationStatus.APPROVED);
    }

    @GetMapping("/member/{memberId}")
    public Page<PublicationVisitorDto> getPublicationsByMember(
            @PathVariable Integer memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PublicationType type,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);

        Page<Publication> publications = publicationRepository.searchMemberPublications(
                memberId,
                normalizeSearch(search),
                normalizeSearch(author),
                year,
                type,
                PublicationStatus.APPROVED,
                pageable
        );

        return publications.map(this::toVisitorDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicationVisitorDto> getPublicationById(@PathVariable Integer id) {
        return publicationRepository.findByIdAndStatus(id, PublicationStatus.APPROVED)
                .map(publication -> ResponseEntity.ok(toVisitorDto(publication)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my-pending")
    @PreAuthorize("hasAnyRole('MEMBER', 'LAB_HEAD')")
    public ResponseEntity<List<MemberPublicationDTO>> getMyPendingPublications(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                publicationValidationService.findPendingPublicationsForAuthenticatedMember(principal.getId())
        );
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MEMBER', 'LAB_HEAD')")
    public ResponseEntity<MemberPublicationDTO> approvePublication(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(publicationValidationService.approvePublication(principal.getId(), id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MEMBER', 'LAB_HEAD')")
    public ResponseEntity<MemberPublicationDTO> rejectPublication(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(publicationValidationService.rejectPublication(principal.getId(), id));
    }

    @PatchMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('MEMBER', 'LAB_HEAD')")
    public ResponseEntity<MemberPublicationDTO> validatePublication(
            @PathVariable Integer id,
            @Valid @RequestBody MemberPublicationValidationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                publicationValidationService.validatePublication(principal.getId(), id, request.getAction())
        );
    }

    private PublicationVisitorDto toVisitorDto(Publication publication) {
        return new PublicationVisitorDto(
                publication.getTitle(),
                publication.getPublicationYear(),
                publication.getAuthorsText(),
                publication.getSource(),
                publication.getExternalUrl(),
                publication.getPdfUrl(),
                publication.getType()
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