ALTER TABLE subscribers
    ADD COLUMN IF NOT EXISTS unsubscribe_token VARCHAR(100);

UPDATE subscribers
SET unsubscribe_token = LOWER(MD5(RANDOM()::TEXT || CLOCK_TIMESTAMP()::TEXT || email))
WHERE unsubscribe_token IS NULL OR TRIM(unsubscribe_token) = '';

ALTER TABLE subscribers
    ALTER COLUMN unsubscribe_token SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_subscribers_unsubscribe_token
    ON subscribers (unsubscribe_token);

CREATE TABLE IF NOT EXISTS subscriber_content_notifications (
                                                                id BIGSERIAL PRIMARY KEY,
                                                                subscriber_id INTEGER NOT NULL,
                                                                content_type VARCHAR(30) NOT NULL,
    content_id INTEGER NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_subscriber_content_notifications_subscriber
    FOREIGN KEY (subscriber_id)
    REFERENCES subscribers(id)
    ON DELETE CASCADE,

    CONSTRAINT chk_subscriber_content_notifications_type
    CHECK (content_type IN ('PUBLICATION', 'PROJECT', 'EVENT')),

    CONSTRAINT uk_subscriber_content_notification_once
    UNIQUE (subscriber_id, content_type, content_id)
    );

CREATE INDEX IF NOT EXISTS idx_subscriber_content_notifications_subscriber_id
    ON subscriber_content_notifications (subscriber_id);

CREATE INDEX IF NOT EXISTS idx_subscriber_content_notifications_content
    ON subscriber_content_notifications (content_type, content_id);