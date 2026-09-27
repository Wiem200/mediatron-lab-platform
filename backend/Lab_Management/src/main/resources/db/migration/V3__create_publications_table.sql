CREATE TABLE IF NOT EXISTS publications (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    doi VARCHAR(255),
    pub_year INTEGER,
    authors_raw TEXT,
    source VARCHAR(255),
    link TEXT,
    type VARCHAR(30) DEFAULT 'OTHER',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_publications_doi'
    ) THEN
        ALTER TABLE publications ADD CONSTRAINT uk_publications_doi UNIQUE (doi);
    END IF;
END $$;
