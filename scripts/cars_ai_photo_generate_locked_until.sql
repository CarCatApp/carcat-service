-- Safe if ddl-auto already added the column.
ALTER TABLE cars ADD COLUMN IF NOT EXISTS ai_photo_generate_locked_until timestamp;
