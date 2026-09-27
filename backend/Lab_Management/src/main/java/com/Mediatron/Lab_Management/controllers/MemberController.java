package com.Mediatron.Lab_Management.controllers;

import com.Mediatron.Lab_Management.authentication.UserPrincipal;
import com.Mediatron.Lab_Management.data_dto.MemberDTO;
import com.Mediatron.Lab_Management.data_dto.MemberProjectSummaryDTO;
import com.Mediatron.Lab_Management.data_dto.MemberPublicationDTO;
import com.Mediatron.Lab_Management.data_dto.MemberUpsertRequest;
import com.Mediatron.Lab_Management.data_dto.PublicMemberCardDTO;
import com.Mediatron.Lab_Management.data_dto.PublicMemberProfileDTO;
import com.Mediatron.Lab_Management.data_models.ERole;
import com.Mediatron.Lab_Management.data_models.Role;
import com.Mediatron.Lab_Management.data_models.User;
import com.Mediatron.Lab_Management.repositories.RoleRepository;
import com.Mediatron.Lab_Management.repositories.UserRepository;
import com.Mediatron.Lab_Management.services.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/members")
@CrossOrigin(origins = "*")
public class MemberController {

    private final MemberService memberService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public MemberController(
            MemberService memberService,
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        this.memberService = memberService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @GetMapping({"", "/all"})
    public List<MemberDTO> getAllMembers() {
        return memberService.findAll();
    }

    @GetMapping("/public")
    public List<PublicMemberCardDTO> getPublicMembers() {
        return memberService.findPublicDirectory();
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<PublicMemberProfileDTO> getPublicMemberById(@PathVariable Integer id) {
        return ResponseEntity.ok(memberService.findPublicProfile(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemberDTO> getMemberById(@PathVariable Integer id) {
        return ResponseEntity.ok(memberService.findById(id));
    }

    @GetMapping("/{id}/projects")
    public ResponseEntity<List<MemberProjectSummaryDTO>> getMemberProjects(@PathVariable Integer id) {
        return ResponseEntity.ok(memberService.findProjects(id));
    }

    @GetMapping("/{id}/publications")
    public ResponseEntity<List<MemberPublicationDTO>> getMemberPublications(@PathVariable Integer id) {
        return ResponseEntity.ok(memberService.findPublications(id));
    }

    @GetMapping("/me/publications")
    @PreAuthorize("hasAnyRole('MEMBER', 'LAB_HEAD')")
    public ResponseEntity<List<MemberPublicationDTO>> getMyPublications(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(memberService.findOwnPublications(principal.getId()));
    }

    @PostMapping({"", "/add"})
    @PreAuthorize("@delegationService.hasAdminPrivileges(authentication)")
    public ResponseEntity<MemberDTO> addMember(@Valid @RequestBody MemberUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.create(request));
    }

    @PutMapping({"/{id}", "/update/{id}"})
    @PreAuthorize("@delegationService.hasAdminPrivileges(authentication)")
    public ResponseEntity<MemberDTO> updateMember(@PathVariable Integer id, @Valid @RequestBody MemberUpsertRequest request) {
        return ResponseEntity.ok(memberService.update(id, request));
    }

    @DeleteMapping({"/{id}", "/delete/{id}"})
    @PreAuthorize("@delegationService.hasAdminPrivileges(authentication)")
    public ResponseEntity<?> deleteMember(
            @PathVariable Integer id,
            @RequestParam(name = "confirm", defaultValue = "false") boolean confirm
    ) {
        return ResponseEntity.ok(memberService.delete(id, confirm));
    }

    @PutMapping("/{id}/change-role")
    @PreAuthorize("@delegationService.hasAdminPrivileges(authentication)")
    public ResponseEntity<?> changeRole(@PathVariable Integer id, @RequestParam ERole newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found."));

        Set<Role> roles = Set.of(roleRepository.findByName(newRole)
                .orElseThrow(() -> new java.util.NoSuchElementException("Role not found.")));

        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(java.util.Map.of(
                "message", "Role updated successfully.",
                "userId", user.getId(),
                "role", newRole.name()
        ));
    }
}
