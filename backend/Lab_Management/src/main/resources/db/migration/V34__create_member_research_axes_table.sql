-- V34__create_member_research_axes_table.sql
-- Fix missing ElementCollection table for Member.researchAxes.

CREATE TABLE IF NOT EXISTS member_research_axes (
                                                    member_id INTEGER NOT NULL,
                                                    research_axes VARCHAR(255) NOT NULL
    );

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_member_research_axes_member'
    ) THEN
ALTER TABLE member_research_axes
    ADD CONSTRAINT fk_member_research_axes_member
        FOREIGN KEY (member_id) REFERENCES members(id)
            ON DELETE CASCADE;
END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_member_research_axes_member_axis'
    ) THEN
ALTER TABLE member_research_axes
    ADD CONSTRAINT uk_member_research_axes_member_axis
        UNIQUE (member_id, research_axes);
END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_member_research_axes_member_id
    ON member_research_axes(member_id);