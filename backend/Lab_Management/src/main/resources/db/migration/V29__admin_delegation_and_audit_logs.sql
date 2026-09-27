CREATE TABLE admin_delegation (
    id SERIAL PRIMARY KEY,
    lab_head_id INTEGER NOT NULL UNIQUE REFERENCES users(id),
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    activated_at TIMESTAMP
);

CREATE INDEX idx_admin_delegation_active ON admin_delegation(is_active);

CREATE TABLE audit_logs (
    id SERIAL PRIMARY KEY,
    actor_id INTEGER NOT NULL REFERENCES users(id),
    action VARCHAR(120) NOT NULL,
    target_id INTEGER,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_actor_timestamp ON audit_logs(actor_id, timestamp DESC);
