-- V35__align_members_table_with_member_entity.sql
-- Fix members schema after merge so it matches the Member entity.

ALTER TABLE members
    ADD COLUMN IF NOT EXISTS user_id INTEGER;

ALTER TABLE members
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(255);

ALTER TABLE members
    ADD COLUMN IF NOT EXISTS last_name VARCHAR(255);

ALTER TABLE members
    ADD COLUMN IF NOT EXISTS grade VARCHAR(255);

ALTER TABLE members
    ADD COLUMN IF NOT EXISTS lab VARCHAR(255) DEFAULT 'MEDIATRON';

ALTER TABLE members
    ADD COLUMN IF NOT EXISTS orcid_id VARCHAR(255);

ALTER TABLE members
    ADD COLUMN IF NOT EXISTS bio TEXT;

ALTER TABLE members
    ADD COLUMN IF NOT EXISTS photo_url TEXT;

ALTER TABLE members
    ADD COLUMN IF NOT EXISTS phone VARCHAR(50);

ALTER TABLE members
    ADD COLUMN IF NOT EXISTS linkedin_url TEXT;

ALTER TABLE members
    ADD COLUMN IF NOT EXISTS google_scholar_url TEXT;

ALTER TABLE members
    ADD COLUMN IF NOT EXISTS personal_site_url TEXT;

ALTER TABLE members
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE members
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;


UPDATE members
SET first_name = 'Unknown'
WHERE first_name IS NULL OR first_name = '';

UPDATE members
SET last_name = 'Member'
WHERE last_name IS NULL OR last_name = '';

UPDATE members
SET grade = 'Member'
WHERE grade IS NULL OR grade = '';

UPDATE members
SET lab = 'MEDIATRON'
WHERE lab IS NULL OR lab = '';

UPDATE members
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

UPDATE members
SET updated_at = CURRENT_TIMESTAMP
WHERE updated_at IS NULL;


ALTER TABLE members
    ALTER COLUMN first_name SET NOT NULL;

ALTER TABLE members
    ALTER COLUMN last_name SET NOT NULL;

ALTER TABLE members
    ALTER COLUMN grade SET NOT NULL;

ALTER TABLE members
    ALTER COLUMN lab SET DEFAULT 'MEDIATRON';

ALTER TABLE members
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE members
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE members
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE members
    ALTER COLUMN updated_at SET NOT NULL;


DO $$
BEGIN
    IF to_regclass('public.users') IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_members_user'
        ) THEN
ALTER TABLE members
    ADD CONSTRAINT fk_members_user
        FOREIGN KEY (user_id) REFERENCES users(id)
            ON DELETE SET NULL;
END IF;
END IF;
END $$;


CREATE INDEX IF NOT EXISTS idx_members_user_id
    ON members(user_id);

CREATE INDEX IF NOT EXISTS idx_members_first_name
    ON members(first_name);

CREATE INDEX IF NOT EXISTS idx_members_last_name
    ON members(last_name);

CREATE INDEX IF NOT EXISTS idx_members_grade
    ON members(grade);
