-- Partner panel "Xidməti tamamla" (2026-10-18).
-- New columns are added by ddl-auto=update; this file documents them and drops the NOT NULL
-- that update cannot change. VisitPanelSchemaFix runs the ALTER on boot as well.

ALTER TABLE visits ALTER COLUMN hyper_record_id DROP NOT NULL;

ALTER TABLE visits ADD COLUMN IF NOT EXISTS source varchar(16);
ALTER TABLE visits ADD COLUMN IF NOT EXISTS booking_id bigint;
ALTER TABLE visits ADD COLUMN IF NOT EXISTS branch_id bigint;
ALTER TABLE visits ADD COLUMN IF NOT EXISTS package_name varchar(80);
ALTER TABLE visits ADD COLUMN IF NOT EXISTS package_price numeric(12, 2);
ALTER TABLE visits ADD COLUMN IF NOT EXISTS discount_percent numeric(5, 2);
ALTER TABLE visits ADD COLUMN IF NOT EXISTS inspection_note varchar(1000);

ALTER TABLE visit_service_lines ADD COLUMN IF NOT EXISTS kind varchar(16);
ALTER TABLE visit_service_lines ADD COLUMN IF NOT EXISTS offered_service_id bigint;
ALTER TABLE visit_service_lines ADD COLUMN IF NOT EXISTS individual_service_id bigint;
ALTER TABLE visit_service_lines ADD COLUMN IF NOT EXISTS done_date date;
ALTER TABLE visit_service_lines ADD COLUMN IF NOT EXISTS done_km integer;
ALTER TABLE visit_service_lines ADD COLUMN IF NOT EXISTS service_amount numeric(12, 2);
ALTER TABLE visit_service_lines ADD COLUMN IF NOT EXISTS discount_percent numeric(5, 2);
ALTER TABLE visit_service_lines ADD COLUMN IF NOT EXISTS final_amount numeric(12, 2);
ALTER TABLE visit_service_lines ADD COLUMN IF NOT EXISTS product_good_id bigint;
ALTER TABLE visit_service_lines ADD COLUMN IF NOT EXISTS product_name varchar(200);
ALTER TABLE visit_service_lines ADD COLUMN IF NOT EXISTS product_qty numeric(10, 2);
ALTER TABLE visit_service_lines ADD COLUMN IF NOT EXISTS product_amount numeric(12, 2);
ALTER TABLE visit_service_lines ADD COLUMN IF NOT EXISTS notes varchar(1000);

ALTER TABLE visit_parts ADD COLUMN IF NOT EXISTS price numeric(12, 2);

ALTER TABLE branch_serviced_goods ADD COLUMN IF NOT EXISTS price numeric(10, 2);
ALTER TABLE offered_services ADD COLUMN IF NOT EXISTS maintenance_code varchar(48);
ALTER TABLE individual_services ADD COLUMN IF NOT EXISTS maintenance_code varchar(48);
