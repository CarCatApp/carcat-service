-- Daha önce name kolonuna İngilizce insert edildi. Yeni jar kalkmadan çalıştır.
-- name -> name_en, name_az ve name_ru dolar.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'individual_service_filters'
          AND column_name = 'name'
    ) THEN
        ALTER TABLE individual_service_filters RENAME COLUMN name TO name_en;
    END IF;
END $$;

ALTER TABLE individual_service_filters ADD COLUMN IF NOT EXISTS name_en varchar(64);
ALTER TABLE individual_service_filters ADD COLUMN IF NOT EXISTS name_az varchar(64);
ALTER TABLE individual_service_filters ADD COLUMN IF NOT EXISTS name_ru varchar(64);

ALTER TABLE individual_service_filters DROP CONSTRAINT IF EXISTS uk_individual_service_filters_name;

UPDATE individual_service_filters SET name_az = 'Filtrlər',    name_en = 'Filters',    name_ru = 'Фильтры'    WHERE id = 1;
UPDATE individual_service_filters SET name_az = 'Əyləclər',    name_en = 'Brakes',     name_ru = 'Тормоза'    WHERE id = 2;
UPDATE individual_service_filters SET name_az = 'Mayelər',     name_en = 'Fluids',     name_ru = 'Жидкости'   WHERE id = 3;
UPDATE individual_service_filters SET name_az = 'Təkərlər',    name_en = 'Tyres',      name_ru = 'Шины'       WHERE id = 4;
UPDATE individual_service_filters SET name_az = 'Alışdırma',   name_en = 'Ignition',   name_ru = 'Зажигание'  WHERE id = 5;
UPDATE individual_service_filters SET name_az = 'Mühərrik',    name_en = 'Engine',     name_ru = 'Двигатель'  WHERE id = 6;
UPDATE individual_service_filters SET name_az = 'Elektrik',    name_en = 'Electrical', name_ru = 'Электрика'  WHERE id = 7;

ALTER TABLE individual_service_filters ALTER COLUMN name_en SET NOT NULL;
ALTER TABLE individual_service_filters ALTER COLUMN name_az SET NOT NULL;
ALTER TABLE individual_service_filters ALTER COLUMN name_ru SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_individual_service_filters_name_en'
    ) THEN
        ALTER TABLE individual_service_filters
            ADD CONSTRAINT uk_individual_service_filters_name_en UNIQUE (name_en);
    END IF;
END $$;
