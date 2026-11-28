-- Promo snapshot on a booking. Hibernate ddl-auto=update also adds these columns.
-- Not run on boot. Empty means the booking did not arrive with a promo.

ALTER TABLE bookings ADD COLUMN IF NOT EXISTS promo_code VARCHAR(64);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS discount_type VARCHAR(16);
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS discount_value NUMERIC(12, 2);
