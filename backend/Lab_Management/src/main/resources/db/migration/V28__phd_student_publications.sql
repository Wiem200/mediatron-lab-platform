CREATE TABLE IF NOT EXISTS phd_student_publications (
    phd_student_id INTEGER NOT NULL REFERENCES phd_students(id) ON DELETE CASCADE,
    publication_id INTEGER NOT NULL REFERENCES publications(id) ON DELETE CASCADE,
    PRIMARY KEY (phd_student_id, publication_id)
);

CREATE INDEX IF NOT EXISTS idx_phd_student_publications_publication
    ON phd_student_publications(publication_id);
