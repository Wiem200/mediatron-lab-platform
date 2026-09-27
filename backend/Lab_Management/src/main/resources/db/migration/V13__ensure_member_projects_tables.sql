CREATE TABLE IF NOT EXISTS projects (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_year INTEGER,
    end_year INTEGER,
    external_url TEXT
);

CREATE TABLE IF NOT EXISTS member_projects (
    member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    project_id INTEGER NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    PRIMARY KEY (member_id, project_id)
);
