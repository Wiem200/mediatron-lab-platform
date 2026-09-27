-- V33__add_created_at_to_events.sql
-- Fix events schema after merge: Event entity expects created_at.

ALTER TABLE events
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;