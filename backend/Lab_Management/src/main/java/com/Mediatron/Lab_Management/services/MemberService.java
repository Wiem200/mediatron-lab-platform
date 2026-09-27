package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_dto.MemberDTO;
import com.Mediatron.Lab_Management.data_dto.MemberProjectSummaryDTO;
import com.Mediatron.Lab_Management.data_dto.MemberPublicationDTO;
import com.Mediatron.Lab_Management.data_dto.MemberUpsertRequest;
import com.Mediatron.Lab_Management.data_dto.PhDStudentDTO;
import com.Mediatron.Lab_Management.data_dto.PhDStudentPublicationDTO;
import com.Mediatron.Lab_Management.data_dto.PublicMemberCardDTO;
import com.Mediatron.Lab_Management.data_dto.PublicMemberProfileDTO;
import com.Mediatron.Lab_Management.data_models.AccountStatus;
import com.Mediatron.Lab_Management.data_models.ERole;
import com.Mediatron.Lab_Management.data_models.Member;
import com.Mediatron.Lab_Management.data_models.PhDStudent;
import com.Mediatron.Lab_Management.data_models.Publication;
import com.Mediatron.Lab_Management.data_models.Project;
import com.Mediatron.Lab_Management.data_models.ProjectStatus;
import com.Mediatron.Lab_Management.data_models.Role;
import com.Mediatron.Lab_Management.data_models.User;
import com.Mediatron.Lab_Management.exceptions.DeletionConfirmationRequiredException;
import com.Mediatron.Lab_Management.repositories.MemberPublicationNotificationRepository;
import com.Mediatron.Lab_Management.repositories.MemberPublicationRepository;
import com.Mediatron.Lab_Management.repositories.MemberRepository;
import com.Mediatron.Lab_Management.repositories.PhDStudentRepository;
import com.Mediatron.Lab_Management.repositories.ProjectRepository;
import com.Mediatron.Lab_Management.repositories.PublicationRepository;
import com.Mediatron.Lab_Management.repositories.RoleRepository;
import com.Mediatron.Lab_Management.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MemberService {
    private static final String DEFAULT_LAB = "MEDIATRON";
    private static final String DEFAULT_PASSWORD = "123456";

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProjectRepository projectRepository;
    private final PhDStudentRepository phDStudentRepository;
    private final MemberPublicationRepository memberPublicationRepository;
    private final MemberPublicationNotificationRepository memberPublicationNotificationRepository;
    private final PublicationRepository publicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberPublicationSyncService memberPublicationSyncService;
    private final PublicationValidationService publicationValidationService;

    public MemberService(
            MemberRepository memberRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            ProjectRepository projectRepository,
            PhDStudentRepository phDStudentRepository,
            MemberPublicationRepository memberPublicationRepository,
            MemberPublicationNotificationRepository memberPublicationNotificationRepository,
            PublicationRepository publicationRepository,
            PasswordEncoder passwordEncoder,
            MemberPublicationSyncService memberPublicationSyncService,
            PublicationValidationService publicationValidationService
    ) {
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.projectRepository = projectRepository;
        this.phDStudentRepository = phDStudentRepository;
        this.memberPublicationRepository = memberPublicationRepository;
        this.memberPublicationNotificationRepository = memberPublicationNotificationRepository;
        this.publicationRepository = publicationRepository;
        this.passwordEncoder = passwordEncoder;
        this.memberPublicationSyncService = memberPublicationSyncService;
        this.publicationValidationService = publicationValidationService;
    }

    @Transactional(readOnly = true)
    public List<MemberDTO> findAll() {
        return memberRepository.findAllByOrderByLastNameAscFirstNameAsc().stream()
                .map(member -> toDto(member, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicMemberCardDTO> findPublicDirectory() {
        return memberRepository.findAllByOrderByLastNameAscFirstNameAsc().stream()
                .filter(this::isPublicDirectoryMember)
                .map(this::toPublicCardDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MemberDTO findById(Integer id) {
        return toDto(getRequiredMember(id), true);
    }

    @Transactional(readOnly = true)
    public PublicMemberProfileDTO findPublicProfile(Integer id) {
        Member member = getRequiredMember(id);

        if (!isPublicDirectoryMember(member)) {
            throw new NoSuchElementException("Member not found.");
        }

        return toPublicProfileDto(member);
    }

    @Transactional(readOnly = true)
    public List<MemberProjectSummaryDTO> findProjects(Integer memberId) {
        Member member = getRequiredMember(memberId);

        if (!isPublicDirectoryMember(member)) {
            throw new NoSuchElementException("Member not found.");
        }

        return toApprovedProjectSummaries(member.getProjects());
    }

    @Transactional(readOnly = true)
    public List<MemberPublicationDTO> findPublications(Integer memberId) {
        Member member = getRequiredMember(memberId);

        if (!isPublicDirectoryMember(member)) {
            throw new NoSuchElementException("Member not found.");
        }

        return publicationValidationService.findApprovedPublicationsForMember(memberId);
    }

    @Transactional(readOnly = true)
    public List<MemberPublicationDTO> findOwnPublications(Integer userId) {
        return publicationValidationService.findPublicationsForAuthenticatedMember(userId);
    }

    @Transactional
    public MemberDTO create(MemberUpsertRequest request) {
        Member member = new Member();
        member.setLab(DEFAULT_LAB);
        member.setUser(createMemberUser(normalizeEmail(request.getEmail())));

        applyProfileFields(member, request);
        Member savedMember = memberRepository.save(member);

        syncProjectLinks(savedMember, request.getProjectIds());
        syncSupervisions(savedMember, request.getSupervisedStudentIds());

        return toDto(savedMember, true);
    }

    @Transactional
    public MemberDTO update(Integer id, MemberUpsertRequest request) {
        Member member = getRequiredMember(id);
        applyProfileFields(member, request);
        syncUserEmail(member, normalizeEmail(request.getEmail()));
        syncProjectLinks(member, request.getProjectIds());
        syncSupervisions(member, request.getSupervisedStudentIds());

        return toDto(memberRepository.save(member), true);
    }

    @Transactional
    public Map<String, Object> delete(Integer id, boolean confirm) {
        Member member = getRequiredMember(id);

        if (!confirm) {
            throw new DeletionConfirmationRequiredException(member.getId());
        }

        Integer memberId = member.getId();
        User linkedUser = member.getUser();
        Integer linkedUserId = linkedUser != null ? linkedUser.getId() : null;

        List<Integer> linkedPublicationIds = memberPublicationRepository.findPublicationIdsForMember(memberId);

        int detachedSupervisedStudents = detachSupervisedStudents(member);
        int detachedResponsibleProjects = projectRepository.clearResponsibleMemberByMemberId(memberId);
        int deletedProjectMemberships = memberRepository.deleteProjectMembershipsByMemberId(memberId);
        int deletedResearchAxes = memberRepository.deleteResearchAxesByMemberId(memberId);

        int deletedNotifications = memberPublicationNotificationRepository.deleteByMemberId(memberId);
        int deletedPublicationLinks = memberPublicationRepository.deleteByMemberId(memberId);
        int deletedOrphanPublications = cleanupOrphanPublications(linkedPublicationIds);

        memberRepository.deleteMemberByIdNative(memberId);

        int deletedUserRoles = 0;
        int deletedUserAccount = 0;

        if (linkedUserId != null) {
            deletedUserRoles = userRepository.deleteUserRolesByUserId(linkedUserId);
            deletedUserAccount = userRepository.deleteUserByIdNative(linkedUserId);
        }

        java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("message", "Member deleted successfully.");
        response.put("memberId", id);
        response.put("deleted", true);
        response.put("deletedPublicationLinks", deletedPublicationLinks);
        response.put("deletedNotifications", deletedNotifications);
        response.put("deletedOrphanPublications", deletedOrphanPublications);
        response.put("detachedSupervisedStudents", detachedSupervisedStudents);
        response.put("detachedResponsibleProjects", detachedResponsibleProjects);
        response.put("deletedProjectMemberships", deletedProjectMemberships);
        response.put("deletedResearchAxes", deletedResearchAxes);
        response.put("deletedUserRoles", deletedUserRoles);
        response.put("deletedUserAccount", deletedUserAccount);

        return response;
    }

    private int detachSupervisedStudents(Member member) {
        List<PhDStudent> supervisedStudents = phDStudentRepository.findBySupervisorId(member.getId());

        for (PhDStudent student : supervisedStudents) {
            student.setSupervisor(null);
        }

        if (!supervisedStudents.isEmpty()) {
            phDStudentRepository.saveAll(supervisedStudents);
        }

        return supervisedStudents.size();
    }

    private int detachResponsibleProjects(Member member) {
        List<Project> responsibleProjects = projectRepository.findByResponsibleMember_Id(member.getId());

        for (Project project : responsibleProjects) {
            project.setResponsibleMember(null);
        }

        if (!responsibleProjects.isEmpty()) {
            projectRepository.saveAll(responsibleProjects);
        }

        return responsibleProjects.size();
    }

    private int detachProjectMemberships(Member member) {
        int detachedCount = 0;

        for (Project project : new ArrayList<>(member.getProjects())) {
            project.getMembers().remove(member);
            member.getProjects().remove(project);
            detachedCount++;
        }

        return detachedCount;
    }

    private int cleanupOrphanPublications(List<Integer> linkedPublicationIds) {
        if (linkedPublicationIds == null || linkedPublicationIds.isEmpty()) {
            return 0;
        }

        publicationRepository.deleteValidationHistoryForOrphanPublications(linkedPublicationIds);

        return publicationRepository.deleteOrphanPublicationsByIds(linkedPublicationIds);
    }

    private Member getRequiredMember(Integer id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Member not found."));
    }

    private void applyProfileFields(Member member, MemberUpsertRequest request) {
        member.setFirstName(requireText(request.getFirstName(), "First name is required."));
        member.setLastName(requireText(request.getLastName(), "Last name is required."));
        member.setGrade(requireText(request.getGrade(), "Grade is required."));
        member.setPhone(requireText(request.getPhone(), "Contact phone is required."));
        member.setBio(cleanNullable(request.getBio()));
        member.setOrcidId(cleanNullable(request.getOrcidId()));
        member.setLab(DEFAULT_LAB);
        member.setPersonalSiteUrl(validateUrl(cleanNullable(request.getPersonalSiteUrl()), "Personal website must be a valid URL."));
        member.setLinkedinUrl(validateLinkedInUrl(request.getLinkedinUrl()));
        member.setGoogleScholarUrl(validateGoogleScholarUrl(request.getGoogleScholarUrl()));
        member.setPhotoUrl(validatePhotoUrl(request.getPhotoUrl()));
        member.setResearchAxes(normalizeResearchAxes(request.getResearchAxes()));
    }

    private User createMemberUser(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("A user with this email already exists.");
        }

        Role memberRole = roleRepository.findByName(ERole.ROLE_MEMBER)
                .orElseThrow(() -> new NoSuchElementException("ROLE_MEMBER is not configured."));

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setStatus(AccountStatus.APPROVED);
        user.setRoles(Set.of(memberRole));
        return userRepository.save(user);
    }

    private void syncUserEmail(Member member, String email) {
        User linkedUser = member.getUser();

        if (linkedUser == null) {
            member.setUser(createMemberUser(email));
            return;
        }

        if (!email.equalsIgnoreCase(linkedUser.getEmail()) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("A user with this email already exists.");
        }

        linkedUser.setEmail(email);
    }

    private void syncProjectLinks(Member member, List<Integer> requestedProjectIds) {
        Set<Integer> desiredIds = normalizeIds(requestedProjectIds);
        List<Project> resolvedProjects = desiredIds.isEmpty() ? List.of() : projectRepository.findAllById(desiredIds);

        if (resolvedProjects.size() != desiredIds.size()) {
            throw new NoSuchElementException("One or more selected projects were not found.");
        }

        Set<Project> desiredProjects = new LinkedHashSet<>(resolvedProjects);

        for (Project currentProject : new ArrayList<>(member.getProjects())) {
            if (!desiredProjects.contains(currentProject)) {
                currentProject.getMembers().remove(member);
                member.getProjects().remove(currentProject);
            }
        }

        for (Project project : desiredProjects) {
            project.getMembers().add(member);
            member.getProjects().add(project);
        }
    }

    private void syncSupervisions(Member member, List<Integer> requestedStudentIds) {
        Set<Integer> desiredIds = normalizeIds(requestedStudentIds);
        List<PhDStudent> desiredStudents = desiredIds.isEmpty() ? List.of() : phDStudentRepository.findAllById(desiredIds);

        if (desiredStudents.size() != desiredIds.size()) {
            throw new NoSuchElementException("One or more selected supervised students were not found.");
        }

        for (PhDStudent currentStudent : phDStudentRepository.findBySupervisorId(member.getId())) {
            if (!desiredIds.contains(currentStudent.getId())) {
                currentStudent.setSupervisor(null);
            }
        }

        for (PhDStudent student : desiredStudents) {
            student.setSupervisor(member);
        }
    }

    private Set<String> normalizeResearchAxes(List<String> researchAxes) {
        if (researchAxes == null) {
            throw new IllegalArgumentException("At least one research axis is required.");
        }

        Set<String> normalized = researchAxes.stream()
                .map(value -> cleanNullable(value))
                .filter(value -> value != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("At least one research axis is required.");
        }

        return normalized;
    }

    private Set<Integer> normalizeIds(List<Integer> ids) {
        if (ids == null) {
            return Set.of();
        }

        return ids.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeEmail(String email) {
        String normalized = requireText(email, "Contact email is required.").toLowerCase(Locale.ROOT);

        if (!normalized.endsWith("@supcom.tn") && !normalized.endsWith("@gmail.com")) {
            throw new IllegalArgumentException("Email must be @supcom.tn or @gmail.com for testing.");
        }

        if (!normalized.contains("@")) {
            throw new IllegalArgumentException("Contact email must be a valid email address.");
        }

        return normalized;
    }

    private String requireText(String value, String message) {
        String normalized = cleanNullable(value);

        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private String cleanNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

    private String validatePhotoUrl(String value) {
        String normalized = cleanNullable(value);

        if (normalized == null) {
            return null;
        }

        if (normalized.startsWith("data:image/")) {
            return normalized;
        }

        return validateUrl(normalized, "Photo must be a valid URL or data:image value.");
    }

    private String validateLinkedInUrl(String value) {
        String normalized = validateUrl(cleanNullable(value), "LinkedIn profile must be a valid URL.");

        if (normalized == null) {
            return null;
        }

        URI uri = parseUri(normalized, "LinkedIn profile must be a valid URL.");
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);

        if (!host.contains("linkedin.com")) {
            throw new IllegalArgumentException("LinkedIn profile URL must point to linkedin.com.");
        }

        return normalized;
    }

    private String validateGoogleScholarUrl(String value) {
        String normalized = validateUrl(cleanNullable(value), "Google Scholar profile must be a valid URL.");

        if (normalized == null) {
            return null;
        }

        URI uri = parseUri(normalized, "Google Scholar profile must be a valid URL.");
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);

        if (!host.contains("scholar.google")) {
            throw new IllegalArgumentException("Google Scholar profile URL must point to scholar.google.*.");
        }

        return normalized;
    }

    private String validateUrl(String value, String message) {
        String normalized = cleanNullable(value);

        if (normalized == null) {
            return null;
        }

        if (!normalized.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            normalized = "https://" + normalized;
        }

        URI uri = parseUri(normalized, message);

        if (uri.getScheme() == null || uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    private URI parseUri(String value, String message) {
        try {
            return new URI(value);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(message);
        }
    }

    private MemberDTO toDto(Member member, boolean includeDetails) {
        MemberDTO dto = new MemberDTO();
        dto.setId(member.getId());
        dto.setLab(member.getLab());
        dto.setFirstName(member.getFirstName());
        dto.setLastName(member.getLastName());
        dto.setGrade(member.getGrade());
        dto.setPhotoUrl(member.getPhotoUrl());
        dto.setBio(member.getBio());
        dto.setOrcidId(member.getOrcidId());
        dto.setResearchAxes(new LinkedHashSet<>(member.getResearchAxes()));
        dto.setLinkedinUrl(member.getLinkedinUrl());
        dto.setGoogleScholarUrl(member.getGoogleScholarUrl());
        dto.setPersonalSiteUrl(member.getPersonalSiteUrl());
        dto.setPhone(member.getPhone());
        dto.setCreatedAt(member.getCreatedAt());
        dto.setUpdatedAt(member.getUpdatedAt());

        User user = member.getUser();
        dto.setUserId(user != null ? user.getId() : null);
        dto.setEmail(user != null ? user.getEmail() : null);
        dto.setContact(buildContact(member.getPhone(), user != null ? user.getEmail() : null));
        dto.setRole(user != null
                ? user.getRoles().stream()
                .findFirst()
                .map(role -> role.getName().name())
                .orElse(null)
                : null);

        dto.setPublicationSync(memberPublicationSyncService.buildSyncInfo(member));

        if (includeDetails) {
            dto.setCurrentProjects(toProjectSummaries(member.getProjects()));
            dto.setSupervisedStudents(toPhdDtos(phDStudentRepository.findBySupervisorId(member.getId())));
            dto.setPublications(publicationValidationService.findAllLinkedPublicationsForMember(member.getId()));
        }

        return dto;
    }

    private List<MemberProjectSummaryDTO> toProjectSummaries(Set<Project> projects) {
        return projects.stream()
                .sorted(Comparator.comparing(Project::getTitle, String.CASE_INSENSITIVE_ORDER))
                .map(this::toProjectSummaryDto)
                .toList();
    }

    private List<MemberProjectSummaryDTO> toApprovedProjectSummaries(Set<Project> projects) {
        return projects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.APPROVED)
                .sorted(Comparator.comparing(Project::getTitle, String.CASE_INSENSITIVE_ORDER))
                .map(this::toProjectSummaryDto)
                .toList();
    }

    private MemberProjectSummaryDTO toProjectSummaryDto(Project project) {
        return new MemberProjectSummaryDTO(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getDuration()
        );
    }

    private List<PhDStudentDTO> toPhdDtos(List<PhDStudent> students) {
        return students.stream()
                .sorted(Comparator.comparing(this::formatPhdName, String.CASE_INSENSITIVE_ORDER))
                .map(student -> {
                    PhDStudentDTO dto = new PhDStudentDTO();
                    dto.setId(student.getId());
                    dto.setPhotoUrl(student.getPhotoUrl());
                    dto.setFirstName(student.getFirstName());
                    dto.setLastName(student.getLastName());
                    dto.setFullName(formatPhdName(student));
                    dto.setThesisTopic(student.getThesisTopic());
                    dto.setRegistrationYear(student.getRegistrationYear());
                    dto.setResearchTeam(student.getResearchTeam());
                    dto.setCoSupervisor(student.getCoSupervisor());

                    if (student.getSupervisor() != null) {
                        dto.setSupervisorId(student.getSupervisor().getId());
                        dto.setSupervisorName(formatSupervisorName(student.getSupervisor()));
                    }

                    dto.setPublicationIds(student.getPublications().stream()
                            .map(Publication::getId)
                            .toList());

                    dto.setPublications(student.getPublications().stream()
                            .sorted(Comparator
                                    .comparing(Publication::getPublicationYear, Comparator.nullsLast(Comparator.reverseOrder()))
                                    .thenComparing(Publication::getTitle, String.CASE_INSENSITIVE_ORDER))
                            .map(publication -> new PhDStudentPublicationDTO(
                                    publication.getId(),
                                    publication.getTitle(),
                                    publication.getPublicationYear()
                            ))
                            .toList());

                    return dto;
                })
                .toList();
    }

    private PublicMemberCardDTO toPublicCardDto(Member member) {
        PublicMemberCardDTO dto = new PublicMemberCardDTO();
        dto.setId(member.getId());
        dto.setPhotoUrl(member.getPhotoUrl());
        dto.setFirstName(member.getFirstName());
        dto.setLastName(member.getLastName());
        dto.setGrade(member.getGrade());
        dto.setContact(buildContact(member.getPhone(), member.resolveEmail()));
        dto.setPersonalSiteUrl(member.getPersonalSiteUrl());
        dto.setLinkedinUrl(member.getLinkedinUrl());
        dto.setGoogleScholarUrl(member.getGoogleScholarUrl());

        return dto;
    }

    private PublicMemberProfileDTO toPublicProfileDto(Member member) {
        PublicMemberProfileDTO dto = new PublicMemberProfileDTO();
        dto.setId(member.getId());
        dto.setPhotoUrl(member.getPhotoUrl());
        dto.setFirstName(member.getFirstName());
        dto.setLastName(member.getLastName());
        dto.setGrade(member.getGrade());
        dto.setContact(buildContact(member.getPhone(), member.resolveEmail()));
        dto.setPersonalSiteUrl(member.getPersonalSiteUrl());
        dto.setLinkedinUrl(member.getLinkedinUrl());
        dto.setGoogleScholarUrl(member.getGoogleScholarUrl());
        dto.setBio(member.getBio());
        dto.setResearchAxes(new LinkedHashSet<>(member.getResearchAxes()));
        dto.setPublications(publicationValidationService.findApprovedPublicationsForMember(member.getId()));
        dto.setCurrentProjects(toApprovedProjectSummaries(member.getProjects()));
        dto.setSupervisions(toPhdDtos(phDStudentRepository.findBySupervisorId(member.getId())));
        dto.setPublicationSync(memberPublicationSyncService.buildSyncInfo(member));

        return dto;
    }

    private boolean isPublicDirectoryMember(Member member) {
        return member != null && !hasRole(member, ERole.ROLE_ADMIN);
    }

    private boolean hasRole(Member member, ERole roleName) {
        User user = member.getUser();

        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }

        return user.getRoles().stream()
                .anyMatch(role -> role != null && roleName.equals(role.getName()));
    }

    private String buildContact(String phone, String email) {
        String normalizedPhone = cleanNullable(phone);
        String normalizedEmail = cleanNullable(email);

        if (normalizedPhone != null && normalizedEmail != null) {
            return normalizedPhone + " | " + normalizedEmail;
        }

        if (normalizedPhone != null) {
            return normalizedPhone;
        }

        return normalizedEmail;
    }

    private String formatMemberName(Member member) {
        String firstName = member.getFirstName() == null ? "" : member.getFirstName().trim();
        String lastName = member.getLastName() == null ? "" : member.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();

        if (!fullName.isEmpty()) {
            return fullName;
        }

        String email = member.resolveEmail();

        return email != null ? email.toLowerCase(Locale.ROOT) : "Member #" + member.getId();
    }

    private String formatSupervisorName(Member member) {
        String grade = cleanNullable(member.getGrade());
        String fullName = formatMemberName(member);

        return grade == null ? fullName : grade + " " + fullName;
    }

    private String formatPhdName(PhDStudent student) {
        String firstName = student.getFirstName() == null ? "" : student.getFirstName().trim();
        String lastName = student.getLastName() == null ? "" : student.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();

        return fullName.isEmpty() ? "PhD Student #" + student.getId() : fullName;
    }
}