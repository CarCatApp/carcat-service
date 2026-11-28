-- promo banner stores context. Hibernate ddl-auto=update also adds this column.
-- Not run on boot. Safe if the column already exists.

ALTER TABLE banners ADD COLUMN IF NOT EXISTS context VARCHAR(4000);
