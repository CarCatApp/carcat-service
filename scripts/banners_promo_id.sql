-- Promo-type banner points at one promo. Hibernate ddl-auto=update also adds the column.
-- Not run on boot. Several nulls are allowed. One promo id can belong to only one banner.

ALTER TABLE banners ADD COLUMN IF NOT EXISTS promo_id BIGINT;
CREATE UNIQUE INDEX IF NOT EXISTS uk_banners_promo_id ON banners (promo_id) WHERE promo_id IS NOT NULL;
