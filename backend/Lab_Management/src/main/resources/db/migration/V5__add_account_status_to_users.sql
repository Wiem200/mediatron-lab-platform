-- Add account status support for User.accountStatus enum mapping.
-- Existing users are marked APPROVED to avoid locking out already-created accounts.
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20);

UPDATE users
SET status = 'APPROVED'
WHERE status IS NULL;

ALTER TABLE users ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE users ALTER COLUMN status SET NOT NULL;
