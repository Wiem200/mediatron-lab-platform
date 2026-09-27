CREATE TABLE events (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    event_date TIMESTAMP NOT NULL,
    location VARCHAR(255),
    responsible_person VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING_HEAD_VALIDATION',
    CONSTRAINT chk_events_status
        CHECK (status IN ('PENDING_HEAD_VALIDATION', 'APPROVED', 'REJECTED'))
);
