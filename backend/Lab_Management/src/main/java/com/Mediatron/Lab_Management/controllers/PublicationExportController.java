package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.data_models.PublicationType;
import com.Mediatron.Lab_Management.services.PublicationExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/publications/export")
@CrossOrigin(origins = "*")
public class PublicationExportController {

    private final PublicationExportService publicationExportService;

    public PublicationExportController(PublicationExportService publicationExportService) {
        this.publicationExportService = publicationExportService;
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportPublicationsToCsv(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PublicationType type
    ) {
        byte[] content = publicationExportService.exportPublicationsToCsv(
                search,
                title,
                author,
                year,
                type
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("publications_export.csv")
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(content);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPublicationsToPdf(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PublicationType type
    ) {
        byte[] content = publicationExportService.exportPublicationsToPdf(
                search,
                title,
                author,
                year,
                type
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("publications_export.pdf")
                        .build()
                        .toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    @GetMapping("/member/{memberId}/csv")
    public ResponseEntity<byte[]> exportMemberPublicationsToCsv(
            @PathVariable Integer memberId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PublicationType type
    ) {
        byte[] content = publicationExportService.exportMemberPublicationsToCsv(
                memberId,
                search,
                author,
                year,
                type
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("member_" + memberId + "_publications_export.csv")
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(content);
    }

    @GetMapping("/member/{memberId}/pdf")
    public ResponseEntity<byte[]> exportMemberPublicationsToPdf(
            @PathVariable Integer memberId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) PublicationType type
    ) {
        byte[] content = publicationExportService.exportMemberPublicationsToPdf(
                memberId,
                search,
                author,
                year,
                type
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("member_" + memberId + "_publications_export.pdf")
                        .build()
                        .toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }
}