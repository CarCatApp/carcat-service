-- Per-flag minAvailableVersion (PO: no global version catalog).
-- Already applied. This file does not run on boot.
--
-- 1) Add column (default so existing rows stay visible until mins are computed).
ALTER TABLE feature_flag ADD COLUMN IF NOT EXISTS min_available_version varchar(32);
UPDATE feature_flag SET min_available_version = '0.0.0'
WHERE min_available_version IS NULL OR btrim(min_available_version) = '';

-- 2) After deploy, Java sets each flag's min to the lowest SemVer among app_version
--    rows that had that flag, then keeps only the "current" grid's ENABLED/DISABLED/HIDDEN.
--    version_id and app_version are dropped by drop_app_version.sql (already applied).
