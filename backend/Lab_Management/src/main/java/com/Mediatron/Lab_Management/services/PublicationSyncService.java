package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_dto.PublicationImportResult;
import com.Mediatron.Lab_Management.data_dto.PublicationSyncJobHistoryDto;
import com.Mediatron.Lab_Management.data_models.Member;
import com.Mediatron.Lab_Management.data_models.PublicationSyncJobHistory;
import com.Mediatron.Lab_Management.repositories.MemberRepository;
import com.Mediatron.Lab_Management.repositories.PublicationSyncJobHistoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class PublicationSyncService {

    private final MemberRepository memberRepository;
    private final PublicationImportService publicationImportService;
    private final PublicationSyncJobHistoryRepository publicationSyncJobHistoryRepository;
    private final MemberPublicationNotificationService memberPublicationNotificationService;

    public PublicationSyncService(
            MemberRepository memberRepository,
            PublicationImportService publicationImportService,
            PublicationSyncJobHistoryRepository publicationSyncJobHistoryRepository,
            MemberPublicationNotificationService memberPublicationNotificationService
    ) {
        this.memberRepository = memberRepository;
        this.publicationImportService = publicationImportService;
        this.publicationSyncJobHistoryRepository = publicationSyncJobHistoryRepository;
        this.memberPublicationNotificationService = memberPublicationNotificationService;
    }

    public PublicationImportResult syncSingleMemberPublications(
            Integer memberId,
            String manualQuery,
            String triggeredBy
    ) {
        LocalDateTime startedAt = LocalDateTime.now();

        Member member = memberRepository.findById(memberId).orElse(null);
        String memberName = member != null ? buildMemberName(member) : null;

        try {
            if (member == null) {
                throw new NoSuchElementException("Member not found with id: " + memberId);
            }

            PublicationImportResult result =
                    publicationImportService.importPublicationsForMember(memberId, manualQuery);

            notifyMemberAboutPendingPublications(member.getId());

            LocalDateTime finishedAt = LocalDateTime.now();

            saveHistory(
                    "MANUAL_SINGLE_MEMBER",
                    normalizeTriggeredBy(triggeredBy),
                    memberId,
                    memberName,
                    "SUCCESS",
                    startedAt,
                    finishedAt,
                    1,
                    1,
                    0,
                    result.getTotalFetched(),
                    result.getImportedCount(),
                    result.getLinkedExistingCount(),
                    result.getSkippedCount(),
                    0,
                    null
            );

            return result;
        } catch (Exception ex) {
            LocalDateTime finishedAt = LocalDateTime.now();

            saveHistory(
                    "MANUAL_SINGLE_MEMBER",
                    normalizeTriggeredBy(triggeredBy),
                    memberId,
                    memberName,
                    "FAILED",
                    startedAt,
                    finishedAt,
                    1,
                    0,
                    1,
                    0,
                    0,
                    0,
                    0,
                    1,
                    cleanErrorMessage(ex)
            );

            throw ex;
        }
    }

    public SyncSummary syncAllMembersPublications() {
        LocalDateTime startedAt = LocalDateTime.now();

        List<Member> members = memberRepository.findAll(
                Sort.by(Sort.Order.asc("lastName"), Sort.Order.asc("firstName"))
        );

        int processedMembers = 0;
        int successfulMembers = 0;
        int failedMembers = 0;
        int totalFetched = 0;
        int totalImported = 0;
        int totalLinkedExisting = 0;
        int totalSkipped = 0;

        List<String> errors = new ArrayList<>();

        for (Member member : members) {
            processedMembers++;

            try {
                PublicationImportResult result =
                        publicationImportService.importPublicationsForMember(member.getId(), null);

                notifyMemberAboutPendingPublications(member.getId());

                successfulMembers++;
                totalFetched += result.getTotalFetched();
                totalImported += result.getImportedCount();
                totalLinkedExisting += result.getLinkedExistingCount();
                totalSkipped += result.getSkippedCount();
            } catch (Exception ex) {
                failedMembers++;
                errors.add(
                        "Member " + member.getId()
                                + " (" + buildMemberName(member) + "): "
                                + cleanErrorMessage(ex)
                );
            }
        }

        LocalDateTime finishedAt = LocalDateTime.now();

        SyncSummary summary = new SyncSummary(
                startedAt,
                finishedAt,
                processedMembers,
                successfulMembers,
                failedMembers,
                totalFetched,
                totalImported,
                totalLinkedExisting,
                totalSkipped,
                errors
        );

        saveHistory(
                "SCHEDULED_ALL_MEMBERS",
                "SYSTEM_SCHEDULED",
                null,
                null,
                determineJobStatus(successfulMembers, failedMembers),
                startedAt,
                finishedAt,
                processedMembers,
                successfulMembers,
                failedMembers,
                totalFetched,
                totalImported,
                totalLinkedExisting,
                totalSkipped,
                errors.size(),
                buildErrorDetails(errors)
        );

        return summary;
    }

    public List<PublicationSyncJobHistoryDto> getRecentSyncHistory(Integer memberId) {
        List<PublicationSyncJobHistory> items;

        if (memberId != null) {
            items = publicationSyncJobHistoryRepository
                    .findTop50ByTargetMemberIdOrderByStartedAtDescIdDesc(memberId);
        } else {
            items = publicationSyncJobHistoryRepository
                    .findTop50ByOrderByStartedAtDescIdDesc();
        }

        return items.stream()
                .map(this::toHistoryDto)
                .toList();
    }

    private void notifyMemberAboutPendingPublications(Integer memberId) {
        try {
            int sentCount = memberPublicationNotificationService.notifyPendingPublicationsForMember(memberId);

            if (sentCount > 0) {
                System.out.println(
                        "Publication notification emails sent for member "
                                + memberId
                                + ": "
                                + sentCount
                );
            }
        } catch (Exception error) {
            System.err.println(
                    "Publication notification failed for member "
                            + memberId
                            + ": "
                            + cleanErrorMessage(error)
            );
        }
    }

    private void saveHistory(
            String triggerType,
            String triggeredBy,
            Integer targetMemberId,
            String targetMemberName,
            String jobStatus,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            int processedMembers,
            int successfulMembers,
            int failedMembers,
            int totalFetched,
            int totalImported,
            int totalLinkedExisting,
            int totalSkipped,
            int errorCount,
            String errorDetails
    ) {
        PublicationSyncJobHistory history = new PublicationSyncJobHistory();
        history.setTriggerType(triggerType);
        history.setTriggeredBy(triggeredBy);
        history.setTargetMemberId(targetMemberId);
        history.setTargetMemberName(targetMemberName);
        history.setJobStatus(jobStatus);
        history.setStartedAt(startedAt);
        history.setFinishedAt(finishedAt);
        history.setProcessedMembers(processedMembers);
        history.setSuccessfulMembers(successfulMembers);
        history.setFailedMembers(failedMembers);
        history.setTotalFetched(totalFetched);
        history.setTotalImported(totalImported);
        history.setTotalLinkedExisting(totalLinkedExisting);
        history.setTotalSkipped(totalSkipped);
        history.setErrorCount(errorCount);
        history.setErrorDetails(errorDetails);

        publicationSyncJobHistoryRepository.save(history);
    }

    private PublicationSyncJobHistoryDto toHistoryDto(PublicationSyncJobHistory history) {
        return new PublicationSyncJobHistoryDto(
                history.getId(),
                history.getTriggerType(),
                history.getTriggeredBy(),
                history.getTargetMemberId(),
                history.getTargetMemberName(),
                history.getJobStatus(),
                history.getStartedAt(),
                history.getFinishedAt(),
                history.getProcessedMembers(),
                history.getSuccessfulMembers(),
                history.getFailedMembers(),
                history.getTotalFetched(),
                history.getTotalImported(),
                history.getTotalLinkedExisting(),
                history.getTotalSkipped(),
                history.getErrorCount(),
                history.getErrorDetails()
        );
    }

    private String determineJobStatus(int successfulMembers, int failedMembers) {
        if (failedMembers == 0) {
            return "SUCCESS";
        }

        if (successfulMembers == 0) {
            return "FAILED";
        }

        return "PARTIAL_SUCCESS";
    }

    private String buildErrorDetails(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }

        return String.join(System.lineSeparator(), errors);
    }

    private String normalizeTriggeredBy(String triggeredBy) {
        if (triggeredBy == null || triggeredBy.isBlank()) {
            return "UNKNOWN";
        }

        return triggeredBy.trim();
    }

    private String buildMemberName(Member member) {
        String firstName = member.getFirstName() == null ? "" : member.getFirstName().trim();
        String lastName = member.getLastName() == null ? "" : member.getLastName().trim();

        String fullName = (firstName + " " + lastName).trim();

        return fullName.isBlank() ? "Unknown member" : fullName;
    }

    private String cleanErrorMessage(Exception ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "Unknown error";
        }

        return ex.getMessage().trim();
    }

    public static class SyncSummary {
        private final LocalDateTime startedAt;
        private final LocalDateTime finishedAt;
        private final int processedMembers;
        private final int successfulMembers;
        private final int failedMembers;
        private final int totalFetched;
        private final int totalImported;
        private final int totalLinkedExisting;
        private final int totalSkipped;
        private final List<String> errors;

        public SyncSummary(
                LocalDateTime startedAt,
                LocalDateTime finishedAt,
                int processedMembers,
                int successfulMembers,
                int failedMembers,
                int totalFetched,
                int totalImported,
                int totalLinkedExisting,
                int totalSkipped,
                List<String> errors
        ) {
            this.startedAt = startedAt;
            this.finishedAt = finishedAt;
            this.processedMembers = processedMembers;
            this.successfulMembers = successfulMembers;
            this.failedMembers = failedMembers;
            this.totalFetched = totalFetched;
            this.totalImported = totalImported;
            this.totalLinkedExisting = totalLinkedExisting;
            this.totalSkipped = totalSkipped;
            this.errors = errors;
        }

        public LocalDateTime getStartedAt() {
            return startedAt;
        }

        public LocalDateTime getFinishedAt() {
            return finishedAt;
        }

        public int getProcessedMembers() {
            return processedMembers;
        }

        public int getSuccessfulMembers() {
            return successfulMembers;
        }

        public int getFailedMembers() {
            return failedMembers;
        }

        public int getTotalFetched() {
            return totalFetched;
        }

        public int getTotalImported() {
            return totalImported;
        }

        public int getTotalLinkedExisting() {
            return totalLinkedExisting;
        }

        public int getTotalSkipped() {
            return totalSkipped;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}