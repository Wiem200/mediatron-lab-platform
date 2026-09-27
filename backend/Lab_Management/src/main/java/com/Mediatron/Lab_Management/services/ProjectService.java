package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_dto.ProjectDTO;
import com.Mediatron.Lab_Management.data_dto.ProjectDocumentLinksDTO;
import com.Mediatron.Lab_Management.data_dto.ProjectValidationRequest;
import com.Mediatron.Lab_Management.data_models.Member;
import com.Mediatron.Lab_Management.data_models.PhDStudent;
import com.Mediatron.Lab_Management.data_models.Project;
import com.Mediatron.Lab_Management.data_models.ProjectStatus;
import com.Mediatron.Lab_Management.data_models.ProjectValidationDecision;
import com.Mediatron.Lab_Management.data_models.ProjectValidationHistory;
import com.Mediatron.Lab_Management.data_models.User;
import com.Mediatron.Lab_Management.exceptions.ProjectDeletionConfirmationRequiredException;
import com.Mediatron.Lab_Management.repositories.MemberRepository;
import com.Mediatron.Lab_Management.repositories.PhDStudentRepository;
import com.Mediatron.Lab_Management.repositories.ProjectRepository;
import com.Mediatron.Lab_Management.repositories.ProjectValidationHistoryRepository;
import com.Mediatron.Lab_Management.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final PhDStudentRepository phDStudentRepository;
    private final UserRepository userRepository;
    private final ProjectValidationHistoryRepository projectValidationHistoryRepository;
    private final SubscriberContentNotificationService subscriberContentNotificationService;
    private final ModelMapper modelMapper;

    public ProjectService(
            ProjectRepository projectRepository,
            MemberRepository memberRepository,
            PhDStudentRepository phDStudentRepository,
            UserRepository userRepository,
            ProjectValidationHistoryRepository projectValidationHistoryRepository,
            SubscriberContentNotificationService subscriberContentNotificationService,
            ModelMapper modelMapper
    ) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.phDStudentRepository = phDStudentRepository;
        this.userRepository = userRepository;
        this.projectValidationHistoryRepository = projectValidationHistoryRepository;
        this.subscriberContentNotificationService = subscriberContentNotificationService;
        this.modelMapper = modelMapper;
        configureProjectDtoMapping();
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> findAll() {
        return projectRepository.findAllByOrderByTitleAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> findAllPublic() {
        return projectRepository.findByStatusOrderByTitleAsc(ProjectStatus.APPROVED).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> findPendingValidation() {
        return projectRepository.findByStatusOrderByTitleAsc(ProjectStatus.PENDING_HEAD_VALIDATION).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDTO findById(Integer id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Project not found."));
        return toDto(project);
    }

    @Transactional(readOnly = true)
    public ProjectDTO findPublicById(Integer id) {
        Project project = projectRepository.findByIdAndStatus(id, ProjectStatus.APPROVED)
                .orElseThrow(() -> new NoSuchElementException("Project not found."));
        return toDto(project);
    }

    @Transactional
    public ProjectDTO create(ProjectDTO dto) {
        Project project = new Project();
        project.setId(null);
        applyChanges(project, dto);
        project.setStatus(ProjectStatus.PENDING_HEAD_VALIDATION);
        return toDto(projectRepository.save(project));
    }

    @Transactional
    public ProjectDTO update(Integer id, ProjectDTO dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Project not found."));
        applyChanges(project, dto);
        project.setStatus(ProjectStatus.PENDING_HEAD_VALIDATION);
        return toDto(projectRepository.save(project));
    }

    @Transactional
    public ProjectDTO validate(Integer id, Integer validatorUserId, ProjectValidationRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Project not found."));

        if (project.getStatus() != ProjectStatus.PENDING_HEAD_VALIDATION) {
            throw new IllegalArgumentException("Only pending projects can be validated.");
        }

        User validator = userRepository.findById(validatorUserId)
                .orElseThrow(() -> new NoSuchElementException("Validator account not found."));

        ProjectValidationDecision decision = parseDecision(request.getDecision());
        String comment = cleanNullable(request.getComment());

        if (decision == ProjectValidationDecision.REJECTED && comment == null) {
            throw new IllegalArgumentException("Scientific feedback is required when rejecting a project.");
        }

        project.setStatus(decision == ProjectValidationDecision.APPROVED ? ProjectStatus.APPROVED : ProjectStatus.REJECTED);
        Project savedProject = projectRepository.save(project);

        ProjectValidationHistory history = new ProjectValidationHistory();
        history.setProject(savedProject);
        history.setDecision(decision);
        history.setComments(comment);
        history.setValidator(validator);
        projectValidationHistoryRepository.save(history);

        notifySubscribersIfProjectPublished(savedProject);

        return toDto(savedProject, Optional.of(history));
    }

    @Transactional
    public Map<String, Object> delete(Integer id, boolean confirm) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Project not found."));

        if (!confirm) {
            throw new ProjectDeletionConfirmationRequiredException(id);
        }

        for (Member member : new ArrayList<>(project.getMembers())) {
            member.getProjects().remove(project);
        }
        project.getMembers().clear();
        project.getPhdStudents().clear();

        projectRepository.delete(project);

        return Map.of(
                "message", "Project deleted successfully.",
                "projectId", id,
                "deleted", true
        );
    }

    private void notifySubscribersIfProjectPublished(Project project) {
        if (project == null || project.getStatus() != ProjectStatus.APPROVED) {
            return;
        }

        try {
            int sentCount = subscriberContentNotificationService.notifyProjectPublished(project);

            if (sentCount > 0) {
                System.out.println(
                        "Subscriber project notification emails sent for project "
                                + project.getId()
                                + ": "
                                + sentCount
                );
            }
        } catch (Exception error) {
            System.err.println(
                    "Subscriber project notification failed for project "
                            + project.getId()
                            + ": "
                            + cleanErrorMessage(error)
            );
        }
    }

    private void applyChanges(Project project, ProjectDTO dto) {
        project.setTitle(requireText(dto.getTitle(), "Project title is required."));
        project.setDescription(requireText(dto.getDescription(), "Project description is required."));
        project.setDuration(requireText(dto.getDuration(), "Project duration is required."));
        project.setFunding(cleanNullable(dto.getFunding()));
        project.setGithubLink(normalizeUrl(dto.getGithubLink()));
        project.setVideoLink(normalizeUrl(dto.getVideoLink()));
        project.setPdfLink(normalizeUrl(dto.getPdfLink()));
        project.setPosterLink(normalizeUrl(dto.getPosterLink()));

        Member responsibleMember = memberRepository.findById(dto.getResponsibleMemberId())
                .orElseThrow(() -> new NoSuchElementException("Responsible member not found."));
        project.setResponsibleMember(responsibleMember);

        Set<Member> resolvedMembers = resolveMembers(dto.getMemberIds());
        resolvedMembers.add(responsibleMember);

        Set<PhDStudent> resolvedPhdStudents = resolvePhdStudents(dto.getPhdStudentIds());

        for (Member current : new ArrayList<>(project.getMembers())) {
            current.getProjects().remove(project);
        }
        project.getMembers().clear();
        for (Member member : resolvedMembers) {
            member.getProjects().add(project);
            project.getMembers().add(member);
        }

        project.getPhdStudents().clear();
        project.getPhdStudents().addAll(resolvedPhdStudents);

        project.setKeywords(normalizeKeywords(dto.getKeywords()));
    }

    private ProjectValidationDecision parseDecision(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "APPROVE", "APPROVED" -> ProjectValidationDecision.APPROVED;
            case "REJECT", "REJECTED" -> ProjectValidationDecision.REJECTED;
            default -> throw new IllegalArgumentException("Decision must be APPROVED or REJECTED.");
        };
    }

    private Set<Member> resolveMembers(List<Integer> memberIds) {
        Set<Integer> ids = memberIds == null
                ? Set.of()
                : memberIds.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (ids.isEmpty()) {
            return new LinkedHashSet<>();
        }

        List<Member> members = memberRepository.findAllById(ids);
        if (members.size() != ids.size()) {
            throw new NoSuchElementException("One or more associated members were not found.");
        }

        return new LinkedHashSet<>(members);
    }

    private Set<PhDStudent> resolvePhdStudents(List<Integer> phdStudentIds) {
        Set<Integer> ids = phdStudentIds == null
                ? Set.of()
                : phdStudentIds.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (ids.isEmpty()) {
            return new LinkedHashSet<>();
        }

        List<PhDStudent> students = phDStudentRepository.findAllById(ids);
        if (students.size() != ids.size()) {
            throw new NoSuchElementException("One or more associated PhD students were not found.");
        }

        return new LinkedHashSet<>(students);
    }

    private String normalizeKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return null;
        }

        return keywords.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(", "));
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

    private String normalizeUrl(String value) {
        String normalized = cleanNullable(value);
        if (normalized == null) {
            return null;
        }

        if (normalized.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
            return normalized;
        }

        return "https://" + normalized;
    }

    private ProjectDTO toDto(Project project) {
        return toDto(project, projectValidationHistoryRepository.findTopByProjectIdOrderByValidatedAtDescIdDesc(project.getId()));
    }

    private ProjectDTO toDto(Project project, Optional<ProjectValidationHistory> latestValidation) {
        ProjectDTO dto = modelMapper.map(project, ProjectDTO.class);
        dto.setKeywords(splitKeywords(project.getKeywords()));

        Member responsibleMember = project.getResponsibleMember();
        if (responsibleMember != null) {
            dto.setResponsibleMemberId(responsibleMember.getId());
            dto.setResponsibleMemberName(formatMemberName(responsibleMember));
            dto.setResponsibleMemberGrade(cleanNullable(responsibleMember.getGrade()));
            dto.setResponsibleMemberPhotoUrl(cleanNullable(responsibleMember.getPhotoUrl()));
            dto.setResponsibleName(formatResponsibleName(responsibleMember));
        }

        List<Member> sortedMembers = project.getMembers().stream()
                .sorted(Comparator.comparing(this::formatMemberName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        dto.setMemberIds(sortedMembers.stream().map(Member::getId).toList());
        dto.setMemberNames(sortedMembers.stream().map(this::formatMemberName).toList());
        dto.setInvolvedMembersNames(new ArrayList<>(dto.getMemberNames()));

        List<PhDStudent> sortedStudents = project.getPhdStudents().stream()
                .sorted(Comparator.comparing(this::formatPhdName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        dto.setPhdStudentIds(sortedStudents.stream().map(PhDStudent::getId).toList());
        dto.setPhdStudentNames(sortedStudents.stream().map(this::formatPhdName).toList());
        dto.setInvolvedPhDsNames(new ArrayList<>(dto.getPhdStudentNames()));
        dto.setDocumentLinks(buildDocumentLinks(project));
        latestValidation.ifPresent(history -> {
            dto.setLatestValidationDecision(history.getDecision());
            dto.setLatestValidationComment(cleanNullable(history.getComments()));
            dto.setLatestValidatedAt(history.getValidatedAt());
            if (history.getValidator() != null) {
                dto.setLatestValidatorId(history.getValidator().getId());
                dto.setLatestValidatorEmail(cleanNullable(history.getValidator().getEmail()));
            }
        });

        return dto;
    }

    private void configureProjectDtoMapping() {
        TypeMap<Project, ProjectDTO> typeMap = modelMapper.getTypeMap(Project.class, ProjectDTO.class);
        if (typeMap != null) {
            return;
        }

        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        modelMapper.createTypeMap(Project.class, ProjectDTO.class).addMappings(mapper -> {
            mapper.skip(ProjectDTO::setResponsibleMemberId);
            mapper.skip(ProjectDTO::setResponsibleMemberName);
            mapper.skip(ProjectDTO::setResponsibleMemberGrade);
            mapper.skip(ProjectDTO::setResponsibleMemberPhotoUrl);
            mapper.skip(ProjectDTO::setResponsibleName);
            mapper.skip(ProjectDTO::setMemberIds);
            mapper.skip(ProjectDTO::setMemberNames);
            mapper.skip(ProjectDTO::setInvolvedMembersNames);
            mapper.skip(ProjectDTO::setPhdStudentIds);
            mapper.skip(ProjectDTO::setPhdStudentNames);
            mapper.skip(ProjectDTO::setInvolvedPhDsNames);
            mapper.skip(ProjectDTO::setKeywords);
            mapper.skip(ProjectDTO::setDocumentLinks);
        });
    }

    private ProjectDocumentLinksDTO buildDocumentLinks(Project project) {
        ProjectDocumentLinksDTO dto = new ProjectDocumentLinksDTO();
        dto.setPdf(buildLink("Technical Report", project.getPdfLink()));
        dto.setPoster(buildLink("Research Poster", project.getPosterLink()));
        dto.setGithub(buildLink("Source Code", project.getGithubLink()));
        dto.setVideo(buildLink("Presentation Video", project.getVideoLink()));
        return dto;
    }

    private ProjectDocumentLinksDTO.LinkItem buildLink(String label, String url) {
        String normalizedUrl = cleanNullable(url);
        if (normalizedUrl == null) {
            return null;
        }

        ProjectDocumentLinksDTO.LinkItem link = new ProjectDocumentLinksDTO.LinkItem();
        link.setLabel(label);
        link.setUrl(normalizedUrl);
        return link;
    }

    private List<String> splitKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return new ArrayList<>();
        }

        return List.of(keywords.split(","))
                .stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
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

    private String formatResponsibleName(Member member) {
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

    private String cleanErrorMessage(Exception ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "Unknown error";
        }

        return ex.getMessage().trim();
    }
}