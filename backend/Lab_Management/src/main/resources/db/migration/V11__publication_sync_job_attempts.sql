ALTER TABLE publication_sync_job_history
    ADD COLUMN IF NOT EXISTS attempts INTEGER NOT NULL DEFAULT 1;

ALTER TABLE publication_sync_job_history
    ADD COLUMN IF NOT EXISTS last_error TEXT;
