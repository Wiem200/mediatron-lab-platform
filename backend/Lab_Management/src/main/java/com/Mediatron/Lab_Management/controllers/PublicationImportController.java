package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.data_dto.PublicationImportResult;
import com.Mediatron.Lab_Management.services.PublicationImportService;
import com.Mediatron.Lab_Management.services.SemanticScholarRateLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/publications/import")
@CrossOrigin(origins = "*")
public class PublicationImportController {

    private final PublicationImportService publicationImportService;

    public PublicationImportController(PublicationImportService publicationImportService) {
        this.publicationImportService = publicationImportService;
    }

    @PostMapping("/member/{memberId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importForMember(
            @PathVariable Integer memberId,
            @RequestParam(required = false) String query
    ) {
        try {
            PublicationImportResult result = publicationImportService.importPublicationsForMember(memberId, query);
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
}