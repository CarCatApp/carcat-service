-- visits: webhook receive timestamps + append-only edit history
-- Safe if ddl-auto already added the columns.

ALTER TABLE visits ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE visits ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE visits ADD COLUMN IF NOT EXISTS edit_history JSONB;
