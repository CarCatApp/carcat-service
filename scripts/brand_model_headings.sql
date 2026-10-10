-- Shared brand headings. Safe to re-run: branch products are deleted only while
-- brand_model_services.branch_id still exists (the old per-branch headings).
-- The new service does the same work on startup. Run this before boot only
-- when the database should be migrated without starting the app.

BEGIN;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = current_schema()
      AND table_name = 'brand_model_services'
      AND column_name = 'branch_id'
  ) THEN
    DELETE FROM brand_models;
    DELETE FROM brand_model_services;
    ALTER TABLE brand_model_services DROP COLUMN branch_id;
  END IF;
END $$;

ALTER TABLE brand_model_services DROP COLUMN IF EXISTS title;
ALTER TABLE brand_model_services ADD COLUMN IF NOT EXISTS title_json varchar(1024);
ALTER TABLE brand_models ADD COLUMN IF NOT EXISTS branch_id bigint;
DROP TABLE IF EXISTS branch_goods;

INSERT INTO brand_model_services (title_json, oil, sort_order)
SELECT v.title_json, v.oil, v.sort_order
FROM (VALUES
  ('{"az":"Yağlar","en":"Oils","ru":"Масла"}', true, 1),
  ('{"az":"Filtrlər","en":"Filters","ru":"Фильтры"}', false, 2),
  ('{"az":"Əyləc qəlibləri","en":"Brake pads","ru":"Тормозные колодки"}', false, 3),
  ('{"az":"Əyləc mayeləri","en":"Brake fluids","ru":"Тормозные жидкости"}', false, 4),
  ('{"az":"Əyləc Diskləri","en":"Brake discs","ru":"Тормозные диски"}', false, 5),
  ('{"az":"Soyutma mayesi (antifriz)","en":"Coolant (antifreeze)","ru":"Охлаждающая жидкость (антифриз)"}', false, 6),
  ('{"az":"Təkərlər","en":"Tires","ru":"Шины"}', false, 7),
  ('{"az":"Qayışlar","en":"Belts","ru":"Ремни"}', false, 8),
  ('{"az":"Akkumulyatorlar","en":"Batteries","ru":"Аккумуляторы"}', false, 9),
  ('{"az":"Digər ehtiyat hissələri","en":"Other spare parts","ru":"Прочие запчасти"}', false, 10)
) AS v(title_json, oil, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM brand_model_services);

COMMIT;
