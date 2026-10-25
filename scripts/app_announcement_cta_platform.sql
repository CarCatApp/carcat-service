-- Separate store links for platform ALL. Hibernate ddl-auto=update also adds these.
-- This file does not run on boot.
-- Redis: app-announcements:candidates is evicted by create/update/setActive/copy after commit.

ALTER TABLE app_announcement ADD COLUMN IF NOT EXISTS cta_url_ios varchar(512);
ALTER TABLE app_announcement ADD COLUMN IF NOT EXISTS cta_url_android varchar(512);
