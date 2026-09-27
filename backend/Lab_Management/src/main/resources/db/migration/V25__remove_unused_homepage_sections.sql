DROP TABLE IF EXISTS scientific_teams;

ALTER TABLE homepage_content
    DROP COLUMN IF EXISTS history_label,
    DROP COLUMN IF EXISTS history_title,
    DROP COLUMN IF EXISTS history_content,
    DROP COLUMN IF EXISTS research_areas_label,
    DROP COLUMN IF EXISTS research_areas_title,
    DROP COLUMN IF EXISTS research_areas_description,
    DROP COLUMN IF EXISTS scientific_teams_label,
    DROP COLUMN IF EXISTS scientific_teams_title,
    DROP COLUMN IF EXISTS scientific_teams_description;
