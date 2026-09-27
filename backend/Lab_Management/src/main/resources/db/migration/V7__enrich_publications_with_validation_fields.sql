ALTER TABLE publications ADD COLUMN IF NOT EXISTS doi VARCHAR(255);
ALTER TABLE publications ADD COLUMN IF NOT EXISTS publication_year INTEGER;
ALTER TABLE publications ADD COLUMN IF NOT EXISTS authors_text TEXT;
ALTER TABLE publications ADD COLUMN IF NOT EXISTS source VARCHAR(255);
ALTER TABLE publications ADD COLUMN IF NOT EXISTS external_url TEXT;
ALTER TABLE publications ADD COLUMN IF NOT EXISTS pdf_url TEXT;
ALTER TABLE publications ADD COLUMN IF NOT EXISTS indexation VARCHAR(255);
ALTER TABLE publications ADD COLUMN IF NOT EXISTS type VARCHAR(30);
ALTER TABLE publications ADD COLUMN IF NOT EXISTS status VARCHAR(20);
ALTER TABLE publications ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE publications ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE publications
SET type = COALESCE(type, 'OTHER'),
    status = COALESCE(status, 'PENDING'),
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP);

ALTER TABLE publications ALTER COLUMN type SET DEFAULT 'OTHER';
ALTER TABLE publications ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE publications ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE publications ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE publications ALTER COLUMN status SET NOT NULL;
ALTER TABLE publications ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE publications ALTER COLUMN updated_at SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_publications_doi'
    ) THEN
        ALTER TABLE publications ADD CONSTRAINT uk_publications_doi UNIQUE (doi);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS publication_validation_history (
    id SERIAL PRIMARY KEY,
    publication_id INTEGER NOT NULL REFERENCES publications(id) ON DELETE CASCADE,
    actor_user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    actor_email VARCHAR(255) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
