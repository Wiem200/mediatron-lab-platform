ALTER TABLE publications
    DROP CONSTRAINT IF EXISTS chk_publication_status;

UPDATE publications
SET status = 'PENDING_MEMBER'
WHERE status = 'PENDING';

ALTER TABLE publications
    ALTER COLUMN status SET DEFAULT 'PENDING_MEMBER';

ALTER TABLE publications
    ADD CONSTRAINT chk_publication_status
    CHECK (status IN ('PENDING_MEMBER', 'PENDING_HEAD', 'APPROVED', 'REJECTED'));
