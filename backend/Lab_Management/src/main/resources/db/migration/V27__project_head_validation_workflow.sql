ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS status VARCHAR(40);

UPDATE projects
SET status = 'APPROVED'
WHERE status IS NULL OR TRIM(status) = '';

ALTER TABLE projects
    ALTER COLUMN status SET DEFAULT 'APPROVED';

ALTER TABLE projects
    ALTER COLUMN status SET NOT NULL;

CREATE TABLE IF NOT EXISTS project_validation_history (
    id BIGSERIAL PRIMARY KEY,
    project_id INTEGER NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    decision VARCHAR(16) NOT NULL,
    comments TEXT,
    validated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    validator_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status);
CREATE INDEX IF NOT EXISTS idx_project_validation_history_project_validated_at
    ON project_validation_history(project_id, validated_at DESC);
