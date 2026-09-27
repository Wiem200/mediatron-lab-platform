package com.Mediatron.Lab_Management.data_dto;

import java.time.LocalDateTime;

public class PublicationSyncJobHistoryDto {

    private Integer id;
    private String triggerType;
    private String triggeredBy;
    private Integer targetMemberId;
    private String targetMemberName;
    private String jobStatus;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private int processedMembers;
    private int successfulMembers;
    private int failedMembers;
    private int totalFetched;
    private int totalImported;
    private int totalLinkedExisting;
    private int totalSkipped;
    private int errorCount;
    private String errorDetails;

    public PublicationSyncJobHistoryDto() {
    }

    public PublicationSyncJobHistoryDto(
            Integer id,
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
        this.id = id;
        this.triggerType = triggerType;
        this.triggeredBy = triggeredBy;
        this.targetMemberId = targetMemberId;
        this.targetMemberName = targetMemberName;
        this.jobStatus = jobStatus;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.processedMembers = processedMembers;
        this.successfulMembers = successfulMembers;
        this.failedMembers = failedMembers;
        this.totalFetched = totalFetched;
        this.totalImported = totalImported;
        this.totalLinkedExisting = totalLinkedExisting;
        this.totalSkipped = totalSkipped;
        this.errorCount = errorCount;
        this.errorDetails = errorDetails;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public Integer getTargetMemberId() {
        return targetMemberId;
    }

    public void setTargetMemberId(Integer targetMemberId) {
        this.targetMemberId = targetMemberId;
    }

    public String getTargetMemberName() {
        return targetMemberName;
    }

    public void setTargetMemberName(String targetMemberName) {
        this.targetMemberName = targetMemberName;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(String jobStatus) {
        this.jobStatus = jobStatus;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public int getProcessedMembers() {
        return processedMembers;
    }

    public void setProcessedMembers(int processedMembers) {
        this.processedMembers = processedMembers;
    }

    public int getSuccessfulMembers() {
        return successfulMembers;
    }

    public void setSuccessfulMembers(int successfulMembers) {
        this.successfulMembers = successfulMembers;
    }

    public int getFailedMembers() {
        return failedMembers;
    }

    public void setFailedMembers(int failedMembers) {
        this.failedMembers = failedMembers;
    }

    public int getTotalFetched() {
        return totalFetched;
    }

    public void setTotalFetched(int totalFetched) {
        this.totalFetched = totalFetched;
    }

    public int getTotalImported() {
        return totalImported;
    }

    public void setTotalImported(int totalImported) {
        this.totalImported = totalImported;
    }

    public int getTotalLinkedExisting() {
        return totalLinkedExisting;
    }

    public void setTotalLinkedExisting(int totalLinkedExisting) {
        this.totalLinkedExisting = totalLinkedExisting;
    }

    public int getTotalSkipped() {
        return totalSkipped;
    }

    public void setTotalSkipped(int totalSkipped) {
        this.totalSkipped = totalSkipped;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
}