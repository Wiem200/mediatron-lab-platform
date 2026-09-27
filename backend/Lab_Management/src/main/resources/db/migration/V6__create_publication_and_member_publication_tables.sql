CREATE TABLE IF NOT EXISTS publications (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS members_publications (
    member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE CASCADE,
    publication_id INTEGER NOT NULL REFERENCES publications(id) ON DELETE CASCADE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (member_id, publication_id)
);
