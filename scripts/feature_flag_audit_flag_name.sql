-- feature_flag_audit: persist flag identity so history survives soft-delete.
-- flag_id is intentionally NOT a foreign key.
-- Safe if ddl-auto already added the columns.

ALTER TABLE feature_flag_audit ADD COLUMN IF NOT EXISTS flag_id BIGINT;
ALTER TABLE feature_flag_audit ADD COLUMN IF NOT EXISTS flag_name VARCHAR(128);
