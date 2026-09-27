-- V37__fix_publication_status_constraint_after_merge.sql
-- Fix publication status constraint after merge.
-- Current Java enum uses: PENDING, APPROVED, REJECTED.
-- Old database constraint may still expect: PENDING_MEMBER, PENDING_HEAD, APPROVED, REJECTED.

ALTER TABLE publications
DROP CONSTRAINT IF EXISTS chk_publication_status;

UPDATE publications
SET status = 'PENDING'
WHERE status IS NULL
   OR status IN ('PENDING_MEMBER', 'PENDING_HEAD', 'PENDING_VALIDATION');

ALTER TABLE publications
    ALTER COLUMN status SET DEFAULT 'PENDING';

ALTER TABLE publications
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE publications
    ADD CONSTRAINT chk_publication_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));