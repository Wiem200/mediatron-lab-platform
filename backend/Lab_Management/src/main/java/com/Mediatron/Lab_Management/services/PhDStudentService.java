package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_dto.PhDStudentDTO;
import com.Mediatron.Lab_Management.data_dto.PhDStudentPublicationDTO;
import com.Mediatron.Lab_Management.data_dto.PhDStudentUpsertRequest;
import com.Mediatron.Lab_Management.data_models.Member;
import com.Mediatron.Lab_Management.data_models.PhDStudent;
import com.Mediatron.Lab_Management.data_models.Publication;
import com.Mediatron.Lab_Management.data_models.PublicationStatus;
import com.Mediatron.Lab_Management.exceptions.PhDStudentDeletionConfirmationRequiredException;
import com.Mediatron.Lab_Management.repositories.MemberRepository;
import com.Mediatron.Lab_Management.repositories.PhDStudentRepository;
import com.Mediatron.Lab_Management.repositories.PublicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class PhDStudentService {

    private final PhDStudentRepository phDStudentRepository;
    private final MemberRepository memberRepository;
    private final PublicationRepository publicationRepository;

    public PhDStudentService(
            PhDStudentRepository phDStudentRepository,
            MemberRepository memberRepository,
            PublicationRepository publicationRepository
    ) {
        this.phDStudentRepository = phDStudentRepository;
        this.memberRepository = memberRepository;
        this.publicationRepository = publicationRepository;
    }

    @Transactional(readOnly = true)
    public List<PhDStudentDTO> findAll() {
        return phDStudentRepository.findAllByOrderByRegistrationYearDescLastNameAscFirstNameAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PhDStudentPublicationDTO> findPublicationOptions() {
        return publicationRepository.findByStatusOrderByPublicationYearDescTitleAsc(PublicationStatus.APPROVED).stream()
                .map(publication -> new PhDStudentPublicationDTO(
                        publication.getId(),
                        publication.getTitle(),
                        publication.getPublicationYear()
                ))
                .toList();
    }

    @Transactional
    public PhDStudentDTO create(PhDStudentUpsertRequest request) {
        PhDStudent student = new PhDStudent();
        student.setId(null);
        applyChanges(student, request);
        return toDto(phDStudentRepository.save(student));
    }

    @Transactional
    public PhDStudentDTO update(Integer id, PhDStudentUpsertRequest request) {
        PhDStudent student = phDStudentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("PhD student not found."));
        applyChanges(student, request);
        return toDto(phDStudentRepository.save(student));
    }

    @Transactional
    public Map<String, Object> delete(Integer id, boolean confirm) {
        PhDStudent student = phDStudentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("PhD student not found."));

        if (!confirm) {
            throw new PhDStudentDeletionConfirmationRequiredException(id);
        }

        student.getPublications().clear();
        phDStudentRepository.delete(student);

        return Map.of(
                "message", "PhD student deleted successfully.",
                "phdStudentId", id,
                "deleted", true
        );
    }

    private void applyChanges(PhDStudent student, PhDStudentUpsertRequest request) {
        student.setFirstName(requireText(request.getFirstName(), "First name is required."));
        student.setLastName(requireText(request.getLastName(), "Last name is required."));
        student.setPhotoUrl(cleanNullable(request.getPhotoUrl()));
        student.setThesisTopic(requireText(request.getThesisTopic(), "Thesis topic is required."));
        student.setRegistrationYear(request.getRegistrationYear());
        student.setResearchTeam(requireText(request.getResearchTeam(), "Research team is required."));
        student.setCoSupervisor(cleanNullable(request.getCoSupervisor()));

        Member supervisor = memberRepository.findById(request.getSupervisorId())
                .orElseThrow(() -> new NoSuchElementException("Supervisor not found."));
        student.setSupervisor(supervisor);

        student.getPublications().clear();
        student.getPublications().addAll(resolvePublications(request.getPublicationIds()));
    }

    private Set<Publication> resolvePublications(List<Integer> publicationIds) {
        Set<Integer> ids = publicationIds == null
                ? Set.of()
                : publicationIds.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (ids.isEmpty()) {
            return new LinkedHashSet<>();
        }

        List<Publication> publications = publicationRepository.findAllById(ids);
        if (publications.size() != ids.size()) {
            throw new NoSuchElementException("One or more associated publications were not found.");
        }

        return publications.stream()
                .filter(publication -> publication.getStatus() == PublicationStatus.APPROVED)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private PhDStudentDTO toDto(PhDStudent student) {
        PhDStudentDTO dto = new PhDStudentDTO();
        dto.setId(student.getId());
        dto.setPhotoUrl(cleanNullable(student.getPhotoUrl()));
        dto.setFirstName(cleanNullable(student.getFirstName()));
        dto.setLastName(cleanNullable(student.getLastName()));
        dto.setFullName(formatPhdName(student));
        dto.setThesisTopic(cleanNullable(student.getThesisTopic()));
        dto.setRegistrationYear(student.getRegistrationYear());
        dto.setResearchTeam(cleanNullable(student.getResearchTeam()));
        dto.setCoSupervisor(cleanNullable(student.getCoSupervisor()));

        Member supervisor = student.getSupervisor();
        if (supervisor != null) {
            dto.setSupervisorId(supervisor.getId());
            dto.setSupervisorName(formatSupervisorName(supervisor));
        }

        List<Publication> sortedPublications = student.getPublications().stream()
                .sorted(Comparator
                        .comparing(Publication::getPublicationYear, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Publication::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();

        dto.setPublicationIds(sortedPublications.stream().map(Publication::getId).toList());
        dto.setPublications(sortedPublications.stream()
                .map(publication -> new PhDStudentPublicationDTO(
                        publication.getId(),
                        publication.getTitle(),
                        publication.getPublicationYear()
                ))
                .toList());
        return dto;
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

    private String formatPhdName(PhDStudent student) {
        String firstName = cleanNullable(student.getFirstName());
        String lastName = cleanNullable(student.getLastName());
        String fullName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        return fullName.isEmpty() ? "PhD Student #" + student.getId() : fullName;
    }

    private String formatSupervisorName(Member member) {
        String lastName = member.getLastName() == null ? "" : member.getLastName().trim();
        if (!lastName.isEmpty()) {
            return "Pr. " + lastName;
        }

        String fullName = formatMemberName(member);
        if (!fullName.isEmpty()) {
            return "Pr. " + fullName;
        }
        return "Pr.";
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
}
