-- Customer promo flag. Hibernate ddl-auto=update also adds this column.
-- Not run on boot. Safe if the column already exists.
-- A null value is treated as false.

ALTER TABLE customers ADD COLUMN IF NOT EXISTS has_promo BOOLEAN;
UPDATE customers SET has_promo = FALSE WHERE has_promo IS NULL;
ALTER TABLE customers ALTER COLUMN has_promo SET DEFAULT FALSE;
