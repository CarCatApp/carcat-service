-- Banner language and expiry. Hibernate ddl-auto=update also adds these columns.
-- Not run on boot. Safe if the columns already exist.
-- A null lang is treated as az. This update records that for old rows.

ALTER TABLE banners ADD COLUMN IF NOT EXISTS lang VARCHAR(8);
ALTER TABLE banners ADD COLUMN IF NOT EXISTS expire_date DATE;
UPDATE banners SET lang = 'az' WHERE lang IS NULL OR btrim(lang) = '';
