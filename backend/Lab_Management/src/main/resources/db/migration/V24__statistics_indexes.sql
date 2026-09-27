CREATE INDEX IF NOT EXISTS idx_publications_status_year ON publications(status, publication_year);
CREATE INDEX IF NOT EXISTS idx_publications_status_type_year ON publications(status, type, publication_year);
CREATE INDEX IF NOT EXISTS idx_members_publications_publication_id ON members_publications(publication_id);
