-- Drop FIN unique so unverified customers may share a pin.
-- SIMA-verified uniqueness is enforced in application code, not here.
-- Hibernate ddl-auto=update does not drop this; run on the server.
-- Constraint name on prod (2026-08-29): ukglhkj1uprhbfan0mgugihyoav

ALTER TABLE customers DROP CONSTRAINT IF EXISTS ukglhkj1uprhbfan0mgugihyoav;
ALTER TABLE customers DROP CONSTRAINT IF EXISTS customers_pin_key;
