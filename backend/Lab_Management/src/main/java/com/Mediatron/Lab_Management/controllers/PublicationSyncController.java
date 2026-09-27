package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.data_dto.PublicationImportResult;
import com.Mediatron.Lab_Management.data_dto.PublicationSyncJobHistoryDto;
import com.Mediatron.Lab_Management.services.PublicationSyncService;
import com.Mediatron.Lab_Management.services.SemanticScholarRateLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/publications/sync")
@CrossOrigin(origins = "*")
public class PublicationSyncController {

    private final PublicationSyncService publicationSyncService;

    public PublicationSyncController(PublicationSyncService publicationSyncService) {
        this.publicationSyncService = publicationSyncService;
    }

    @PostMapping("/{memberId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> syncMemberPublications(
            @PathVariable Integer memberId,
            @RequestParam(required = false) String query,
            Authentication authentication
    ) {
        try {
            String triggeredBy = authentication != null ? authentication.getName() : "UNKNOWN_ADMIN";

            PublicationImportResult result = publicationSyncService.syncSingleMemberPublications(
                    memberId,
                    query,
                    triggeredBy
            );

            return ResponseEntity.ok(result);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (SemanticScholarRateLimitException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ex.getMessage());
        }
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PublicationSyncJobHistoryDto>> getSyncHistory(
            @RequestParam(required = false) Integer memberId
    ) {
        return ResponseEntity.ok(publicationSyncService.getRecentSyncHistory(memberId));
    }
}