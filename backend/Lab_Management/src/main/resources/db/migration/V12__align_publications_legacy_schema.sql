DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'publications' AND column_name = 'pub_year'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'publications' AND column_name = 'publication_year'
    ) THEN
        ALTER TABLE publications RENAME COLUMN pub_year TO publication_year;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'publications' AND column_name = 'authors_raw'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'publications' AND column_name = 'authors_text'
    ) THEN
        ALTER TABLE publications RENAME COLUMN authors_raw TO authors_text;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'publications' AND column_name = 'link'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'publications' AND column_name = 'external_url'
    ) THEN
        ALTER TABLE publications RENAME COLUMN link TO external_url;
    END IF;
END $$;

ALTER TABLE publications ADD COLUMN IF NOT EXISTS publication_year INTEGER;
ALTER TABLE publications ADD COLUMN IF NOT EXISTS authors_text TEXT;
ALTER TABLE publications ADD COLUMN IF NOT EXISTS external_url TEXT;
ALTER TABLE publications ADD COLUMN IF NOT EXISTS pdf_url TEXT;
ALTER TABLE publications ADD COLUMN IF NOT EXISTS indexation VARCHAR(255);
ALTER TABLE publications ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE publications
SET updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP);

ALTER TABLE publications ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE publications ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE publications ALTER COLUMN type TYPE VARCHAR(30)
USING (
    CASE type::text
        WHEN 'CONF_A' THEN 'CONFERENCE_A'
        WHEN 'CONF_B' THEN 'CONFERENCE_B'
        WHEN 'CONF_C' THEN 'CONFERENCE_C'
        WHEN 'JOURNAL' THEN 'JOURNAL'
        WHEN 'WORKSHOP' THEN 'WORKSHOP'
        WHEN 'BOOK_CHAPTER' THEN 'BOOK_CHAPTER'
        WHEN 'OTHER' THEN 'OTHER'
        ELSE 'OTHER'
    END
);

ALTER TABLE publications ALTER COLUMN status TYPE VARCHAR(20)
USING (
    CASE status::text
        WHEN 'APPROVED' THEN 'APPROVED'
        WHEN 'REJECTED' THEN 'REJECTED'
        WHEN 'PENDING_MEMBER' THEN 'PENDING'
        WHEN 'PENDING_HEAD' THEN 'PENDING'
        WHEN 'PENDING' THEN 'PENDING'
        ELSE 'PENDING'
    END
);

UPDATE publications
SET type = COALESCE(type, 'OTHER'),
    status = COALESCE(status, 'PENDING');

ALTER TABLE publications ALTER COLUMN type SET DEFAULT 'OTHER';
ALTER TABLE publications ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE publications ALTER COLUMN status SET NOT NULL;
