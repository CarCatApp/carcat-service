-- Manual. Does not run on boot.
-- Creates offered_service_filters, adds offered_services.offered_service_filter_id,
-- inserts the 9 groups, then sets the id on existing rows by Azerbaijani title.
-- Safe before or after the jar: CREATE / ALTER are IF NOT EXISTS.

CREATE TABLE IF NOT EXISTS offered_service_filters (
    id       bigserial PRIMARY KEY,
    name_az  varchar(64) NOT NULL,
    name_en  varchar(64) NOT NULL,
    name_ru  varchar(64) NOT NULL,
    CONSTRAINT uk_offered_service_filters_name_en UNIQUE (name_en)
);

ALTER TABLE offered_services
    ADD COLUMN IF NOT EXISTS offered_service_filter_id bigint;

INSERT INTO offered_service_filters (id, name_az, name_en, name_ru) VALUES
    (1, 'Filtrlər',              'Filters',         'Фильтры'),
    (2, 'Əyləclər',              'Brakes',          'Тормоза'),
    (3, 'Soyutma sistemi',       'Cooling system',  'Система охлаждения'),
    (4, 'Zamanlama və ötürücü',  'Timing & drive',  'ГРМ и привод'),
    (5, 'Alışdırma',             'Ignition',        'Зажигание'),
    (6, 'Mayelər',               'Fluids',          'Жидкости'),
    (7, 'Təkərlər',              'Tyres',           'Шины'),
    (8, 'Mühərrik',              'Engine',          'Двигатель'),
    (9, 'Kuzov',                 'Body',            'Кузов')
ON CONFLICT (id) DO UPDATE SET
    name_az = EXCLUDED.name_az,
    name_en = EXCLUDED.name_en,
    name_ru = EXCLUDED.name_ru;

SELECT setval(
    pg_get_serial_sequence('offered_service_filters', 'id'),
    (SELECT MAX(id) FROM offered_service_filters)
);

UPDATE offered_services o
SET offered_service_filter_id = 1
WHERE o.title_json::jsonb ->> 'az' IN (
    'Mühərrikin yağ filtri',
    'Hava filtri',
    'Yanacaq Filtri',
    'Salon (kabin) filtri'
);

UPDATE offered_services o
SET offered_service_filter_id = 2
WHERE o.title_json::jsonb ->> 'az' IN (
    'Əyləc diskləri',
    'Əyləc boruları və şlanqları',
    'Əyləc supportları',
    'Baraban əyləcləri',
    'Əyləc Qəlibləri',
    'Əl əyləci'
);

UPDATE offered_services o
SET offered_service_filter_id = 3
WHERE o.title_json::jsonb ->> 'az' IN (
    'Soyutma mayesi (Antifriz)',
    'Radiator və antifriz şlanqları'
);

UPDATE offered_services o
SET offered_service_filter_id = 4
WHERE o.title_json::jsonb ->> 'az' IN (
    'Paylayıcı qayış',
    'İlişmə Muftası',
    'Köməkçi qayışlar',
    'Generator Qayışı'
);

UPDATE offered_services o
SET offered_service_filter_id = 6
WHERE o.title_json::jsonb ->> 'az' IN (
    'Mühərrik yağı',
    'Şüşəyuyan mayesi',
    'Sükan gücləndirici mayesi',
    'Əyləc mayesi',
    'Akkumulyator mayesi',
    'AdBlue',
    'Transmissiya Mayesi'
);

UPDATE offered_services o
SET offered_service_filter_id = 7
WHERE o.title_json::jsonb ->> 'az' IN (
    'Təkər (podşipniklər)',
    'Təkərlərin vəziyyəti və protektoru',
    'Təkər təzyiqi',
    'Təkər boltlarının yoxlanışı'
);

UPDATE offered_services o
SET offered_service_filter_id = 8
WHERE o.title_json::jsonb ->> 'az' IN (
    'Mühərrik yağı sızmaları',
    'Mühərrikin performansı',
    'Yanacaq sistemi və çəni',
    'Egzozda tüstü yoxlanışı',
    'Egzoz sistemi'
);

UPDATE offered_services o
SET offered_service_filter_id = 9
WHERE o.title_json::jsonb ->> 'az' IN (
    'Güzgülər',
    'Yanacaq çəninin qapağı',
    'Qapı, kapot və kilidlərin işləməsi',
    'Siqnal',
    'Kondisioner',
    'Sükan reykası və hissələri',
    'Amortizatorlar'
);
