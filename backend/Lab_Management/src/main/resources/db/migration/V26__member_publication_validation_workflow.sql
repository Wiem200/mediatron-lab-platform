ALTER TABLE members_publications
    ADD COLUMN IF NOT EXISTS status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE members_publications mp
SET status = CASE
    WHEN mp.is_verified THEN 'APPROVED'
    WHEN p.status = 'REJECTED' THEN 'REJECTED'
    WHEN p.status = 'APPROVED' THEN 'APPROVED'
    ELSE 'PENDING'
END
FROM publications p
WHERE p.id = mp.publication_id
  AND mp.status IS NULL;

UPDATE members_publications
SET status = COALESCE(status, 'PENDING'),
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP);

ALTER TABLE members_publications
    ALTER COLUMN status SET DEFAULT 'PENDING',
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_members_publications_member_status
    ON members_publications(member_id, status);
