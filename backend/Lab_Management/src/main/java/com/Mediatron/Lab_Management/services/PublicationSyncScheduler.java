package com.Mediatron.Lab_Management.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PublicationSyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PublicationSyncScheduler.class);

    private final PublicationSyncService publicationSyncService;

    @Value("${mediatron.publications.sync.enabled:true}")
    private boolean syncEnabled;

    public PublicationSyncScheduler(PublicationSyncService publicationSyncService) {
        this.publicationSyncService = publicationSyncService;
    }

    @Scheduled(cron = "${mediatron.publications.sync.cron:0 0 2 * * *}")
    public void runScheduledPublicationSync() {
        if (!syncEnabled) {
            logger.info("Publication sync skipped because mediatron.publications.sync.enabled=false");
            return;
        }

        logger.info("Starting scheduled publication sync...");

        PublicationSyncService.SyncSummary summary =
                publicationSyncService.syncAllMembersPublications();

        logger.info(
                "Scheduled publication sync finished. processedMembers={}, successfulMembers={}, failedMembers={}, totalFetched={}, totalImported={}, totalLinkedExisting={}, totalSkipped={}, startedAt={}, finishedAt={}",
                summary.getProcessedMembers(),
                summary.getSuccessfulMembers(),
                summary.getFailedMembers(),
                summary.getTotalFetched(),
                summary.getTotalImported(),
                summary.getTotalLinkedExisting(),
                summary.getTotalSkipped(),
                summary.getStartedAt(),
                summary.getFinishedAt()
        );

        if (!summary.getErrors().isEmpty()) {
            logger.warn("Publication sync errors: {}", summary.getErrors());
        }
    }
}