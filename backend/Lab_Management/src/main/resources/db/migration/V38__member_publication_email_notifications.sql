CREATE TABLE IF NOT EXISTS member_publication_notifications (
                                                                id BIGSERIAL PRIMARY KEY,
                                                                member_id INTEGER NOT NULL,
                                                                publication_id INTEGER NOT NULL,
                                                                notification_type VARCHAR(80) NOT NULL DEFAULT 'PUBLICATION_DETECTED',
    recipient_email VARCHAR(255) NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_member_publication_notifications_member
    FOREIGN KEY (member_id)
    REFERENCES members(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_member_publication_notifications_publication
    FOREIGN KEY (publication_id)
    REFERENCES publications(id)
    ON DELETE CASCADE,

    CONSTRAINT uk_member_publication_notification
    UNIQUE (member_id, publication_id, notification_type)
    );

CREATE INDEX IF NOT EXISTS idx_member_publication_notifications_member
    ON member_publication_notifications(member_id);

CREATE INDEX IF NOT EXISTS idx_member_publication_notifications_publication
    ON member_publication_notifications(publication_id);