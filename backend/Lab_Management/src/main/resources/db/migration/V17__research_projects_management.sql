ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS duration VARCHAR(120),
    ADD COLUMN IF NOT EXISTS funding TEXT,
    ADD COLUMN IF NOT EXISTS keywords TEXT,
    ADD COLUMN IF NOT EXISTS github_link TEXT,
    ADD COLUMN IF NOT EXISTS video_link TEXT,
    ADD COLUMN IF NOT EXISTS responsible_member_id INTEGER REFERENCES members(id) ON DELETE SET NULL;

UPDATE projects
SET duration = TRIM(
    CONCAT(
        COALESCE(start_year::TEXT, ''),
        CASE
            WHEN start_year IS NOT NULL AND end_year IS NOT NULL THEN ' - '
            ELSE ''
        END,
        COALESCE(end_year::TEXT, '')
    )
)
WHERE duration IS NULL
  AND (start_year IS NOT NULL OR end_year IS NOT NULL);

UPDATE projects
SET duration = 'Not specified'
WHERE duration IS NULL OR TRIM(duration) = '';

CREATE TABLE IF NOT EXISTS project_members (
    member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    project_id INTEGER NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    PRIMARY KEY (member_id, project_id)
);

INSERT INTO project_members (member_id, project_id)
SELECT member_id, project_id
FROM member_projects
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS project_phds (
    project_id INTEGER NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    phd_student_id INTEGER NOT NULL REFERENCES phd_students(id) ON DELETE CASCADE,
    PRIMARY KEY (project_id, phd_student_id)
);
