CREATE INDEX IF NOT EXISTS idx_publications_publication_year ON publications(publication_year);
CREATE INDEX IF NOT EXISTS idx_publications_type ON publications(type);
CREATE INDEX IF NOT EXISTS idx_publications_status ON publications(status);
CREATE INDEX IF NOT EXISTS idx_publications_doi_lower ON publications((LOWER(doi)));

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_publication_type'
    ) THEN
        ALTER TABLE publications
            ADD CONSTRAINT chk_publication_type
                CHECK (type IN ('JOURNAL', 'CONFERENCE_A', 'CONFERENCE_B', 'CONFERENCE_C', 'WORKSHOP', 'BOOK_CHAPTER', 'OTHER'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_publication_status'
    ) THEN
        ALTER TABLE publications
            ADD CONSTRAINT chk_publication_status
                CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));
    END IF;
END $$;
