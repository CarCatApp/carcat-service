-- Drop leftover global app_version catalog (PO: version is a field on the flag).
-- Boot also runs this in FeatureFlagLegacyCleanup after per-flag min migration.
-- Hibernate ddl-auto=update does not drop tables/columns; this (or boot) is required.

ALTER TABLE feature_flag_role_state DROP CONSTRAINT IF EXISTS uk_ffrs_flag_version_role;
ALTER TABLE feature_flag_role_state DROP COLUMN IF EXISTS version_id CASCADE;
DROP TABLE IF EXISTS app_version CASCADE;

ALTER TABLE feature_flag_role_state DROP CONSTRAINT IF EXISTS uk_ffrs_flag_role;
ALTER TABLE feature_flag_role_state ADD CONSTRAINT uk_ffrs_flag_role UNIQUE (flag_id, role);
