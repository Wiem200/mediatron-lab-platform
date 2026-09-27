package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.data_dto.PhDStudentDTO;
import com.Mediatron.Lab_Management.data_dto.PhDStudentPublicationDTO;
import com.Mediatron.Lab_Management.data_dto.PhDStudentUpsertRequest;
import com.Mediatron.Lab_Management.services.PhDStudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/phd-students")
@CrossOrigin(origins = "*")
public class PhDStudentController {

    private final PhDStudentService phDStudentService;

    public PhDStudentController(PhDStudentService phDStudentService) {
        this.phDStudentService = phDStudentService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<PhDStudentDTO>> getAll() {
        return ResponseEntity.ok(phDStudentService.findAll());
    }

    @GetMapping("/publication-options")
    @PreAuthorize("@delegationService.hasAdminPrivileges(authentication)")
    public ResponseEntity<List<PhDStudentPublicationDTO>> getPublicationOptions() {
        return ResponseEntity.ok(phDStudentService.findPublicationOptions());
    }

    @PostMapping("/add")
    @PreAuthorize("@delegationService.hasAdminPrivileges(authentication)")
    public ResponseEntity<PhDStudentDTO> add(@Valid @RequestBody PhDStudentUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(phDStudentService.create(request));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("@delegationService.hasAdminPrivileges(authentication)")
    public ResponseEntity<PhDStudentDTO> update(
            @PathVariable Integer id,
            @Valid @RequestBody PhDStudentUpsertRequest request
    ) {
        return ResponseEntity.ok(phDStudentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@delegationService.hasAdminPrivileges(authentication)")
    public ResponseEntity<?> delete(
            @PathVariable Integer id,
            @RequestParam(name = "confirm", defaultValue = "false") boolean confirm
    ) {
        return ResponseEntity.ok(phDStudentService.delete(id, confirm));
    }
}
