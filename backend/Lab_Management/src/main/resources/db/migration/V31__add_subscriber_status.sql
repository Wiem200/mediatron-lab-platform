ALTER TABLE subscribers
    ADD COLUMN IF NOT EXISTS status VARCHAR(20);

UPDATE subscribers
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE subscribers
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE subscribers
    ALTER COLUMN status SET NOT NULL;
