package com.Mediatron.Lab_Management.data_models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "publication_sync_job_history")
public class PublicationSyncJobHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "trigger_type", nullable = false, length = 50)
    private String triggerType;

    @Column(name = "triggered_by", nullable = false)
    private String triggeredBy;

    @Column(name = "target_member_id")
    private Integer targetMemberId;

    @Column(name = "target_member_name")
    private String targetMemberName;

    @Column(name = "job_status", nullable = false, length = 30)
    private String jobStatus;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt;

    @Column(name = "processed_members", nullable = false)
    private int processedMembers;

    @Column(name = "successful_members", nullable = false)
    private int successfulMembers;

    @Column(name = "failed_members", nullable = false)
    private int failedMembers;

    @Column(name = "total_fetched", nullable = false)
    private int totalFetched;

    @Column(name = "total_imported", nullable = false)
    private int totalImported;

    @Column(name = "total_linked_existing", nullable = false)
    private int totalLinkedExisting;

    @Column(name = "total_skipped", nullable = false)
    private int totalSkipped;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}