ALTER TABLE publications
    DROP CONSTRAINT IF EXISTS uk_publications_doi;

DROP INDEX IF EXISTS idx_publications_doi_lower;
