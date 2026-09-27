-- Add new columns to members
ALTER TABLE members ADD COLUMN linkedin_url TEXT;
ALTER TABLE members ADD COLUMN google_scholar_url TEXT;
ALTER TABLE members ADD COLUMN personal_site_url TEXT;
ALTER TABLE members RENAME COLUMN position TO grade;

-- Create phd_students table
CREATE TABLE phd_students (
                              id SERIAL PRIMARY KEY,
                              first_name VARCHAR(100) NOT NULL,
                              last_name VARCHAR(100) NOT NULL,
                              photo_url TEXT,
                              thesis_topic TEXT,
                              registration_year INTEGER,
                              research_team VARCHAR(150),
                              supervisor_id INTEGER REFERENCES members(id) ON DELETE SET NULL,
                              co_supervisor VARCHAR(150)
);

-- Table for Research Axes (Member multi-valued attribute)
CREATE TABLE member_research_axes (
                                      member_id INTEGER REFERENCES members(id) ON DELETE CASCADE,
                                      research_axes VARCHAR(255)
);