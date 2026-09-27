package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.authentication.UserPrincipal;
import com.Mediatron.Lab_Management.data_dto.ProjectDTO;
import com.Mediatron.Lab_Management.data_dto.ProjectValidationRequest;
import com.Mediatron.Lab_Management.services.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LAB_HEAD')")
    public List<ProjectDTO> getAllProjects() {
        return projectService.findAll();
    }

    @GetMapping("")
    public List<ProjectDTO> getProjects() {
        return projectService.findAllPublic();
    }

    @GetMapping("/public")
    public List<ProjectDTO> getPublicProjects() {
        return projectService.findAllPublic();
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ProjectDTO> getPublicProjectById(@PathVariable Integer id) {
        return ResponseEntity.ok(projectService.findPublicById(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LAB_HEAD')")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable Integer id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('LAB_HEAD')")
    public ResponseEntity<List<ProjectDTO>> getPendingProjects() {
        return ResponseEntity.ok(projectService.findPendingValidation());
    }

    @PostMapping("")
    @PreAuthorize("@delegationService.hasAdminPrivileges(authentication)")
    public ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody ProjectDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(dto));
    }

    @PostMapping("/add")
    @PreAuthorize("@delegationService.hasAdminPrivileges(authentication)")
    public ResponseEntity<ProjectDTO> addProject(@Valid @RequestBody ProjectDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@delegationService.hasAdminPrivileges(authentication)")
    public ResponseEntity<ProjectDTO> replaceProject(@PathVariable Integer id, @Valid @RequestBody ProjectDTO dto) {
        return ResponseEntity.ok(projectService.update(id, dto));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("@delegationService.hasAdminPrivileges(authentication)")
    public ResponseEntity<ProjectDTO> updateProject(@PathVariable Integer id, @Valid @RequestBody ProjectDTO dto) {
        return ResponseEntity.ok(projectService.update(id, dto));
    }

    @PutMapping("/{id}/validate")
    @PreAuthorize("hasRole('LAB_HEAD')")
    public ResponseEntity<ProjectDTO> validateProject(
            @PathVariable Integer id,
            @Valid @RequestBody ProjectValidationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(projectService.validate(id, principal.getId(), request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@delegationService.hasAdminPrivileges(authentication)")
    public ResponseEntity<?> deleteProject(
            @PathVariable Integer id,
            @RequestParam(name = "confirm", defaultValue = "false") boolean confirm
    ) {
        return ResponseEntity.ok(projectService.delete(id, confirm));
    }
}
