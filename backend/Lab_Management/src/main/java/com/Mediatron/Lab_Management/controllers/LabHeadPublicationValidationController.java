package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.data_dto.PublicationStatusUpdateRequest;
import com.Mediatron.Lab_Management.data_dto.PublicationValidationHistoryDto;
import com.Mediatron.Lab_Management.data_dto.PublicationValidationQueueDto;
import com.Mediatron.Lab_Management.data_models.PublicationStatus;
import com.Mediatron.Lab_Management.data_models.PublicationType;
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
@RequestMapping("/api/lab-head/publications")
@CrossOrigin(origins = "*")
public class LabHeadPublicationValidationController {

    private final PublicationValidationService publicationValidationService;

    public LabHeadPublicationValidationController(PublicationValidationService publicationValidationService) {
        this.publicationValidationService = publicationValidationService;
    }

    @GetMapping("/validation-queue")
    @PreAuthorize("hasAnyRole('LAB_HEAD','ADMIN')")
    public ResponseEntity<Page<PublicationValidationQueueDto>> getValidationQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PublicationType type,
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) PublicationStatus status
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "updatedAt")
                        .and(Sort.by(Sort.Direction.DESC, "publicationYear"))
                        .and(Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        PublicationStatus effectiveStatus = status != null ? status : PublicationStatus.PENDING;

        Page<PublicationValidationQueueDto> result = publicationValidationService.getValidationQueue(
                memberId,
                normalizeSearch(search),
                year,
                type,
                effectiveStatus,
                pageable
        );

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{publicationId}/status")
    @PreAuthorize("hasAnyRole('LAB_HEAD','ADMIN')")
    public ResponseEntity<PublicationValidationQueueDto> updatePublicationStatus(
            @PathVariable Integer publicationId,
            @RequestBody PublicationStatusUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                publicationValidationService.reviewerValidatePublication(
                        publicationId,
                        request,
                        authentication
                )
        );
    }

    @GetMapping("/{publicationId}/history")
    @PreAuthorize("hasAnyRole('LAB_HEAD','ADMIN')")
    public ResponseEntity<List<PublicationValidationHistoryDto>> getPublicationHistory(
            @PathVariable Integer publicationId
    ) {
        return ResponseEntity.ok(
                publicationValidationService.getReviewerPublicationHistory(publicationId)
        );
    }

    private String normalizeSearch(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();
        return cleaned.isBlank() ? "" : cleaned;
    }
}