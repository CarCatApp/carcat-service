-- Elle çalıştır. Boot'ta çalışmaz.
-- 24 canlı katalog satırı. Eski 7 filtre durur; yalnız Qaz sistemi eklenir. Silinen filtre yok.
-- Kod, title_json'daki adın hash'idir. Id'ye göre yazma.

INSERT INTO individual_service_filters (id, name_az, name_en, name_ru) VALUES
    (8, 'Qaz sistemi', 'Gas system', 'Газовая система')
ON CONFLICT (id) DO UPDATE SET
    name_az = EXCLUDED.name_az,
    name_en = EXCLUDED.name_en,
    name_ru = EXCLUDED.name_ru;

SELECT setval(
    pg_get_serial_sequence('individual_service_filters', 'id'),
    (SELECT MAX(id) FROM individual_service_filters)
);

UPDATE individual_services AS s
SET individual_service_filter_id = f.id
FROM individual_service_filters AS f
WHERE f.name_en = 'Filters'
  AND s.code IN ('CB160275', '6ACF1625', '7B77D15C', '852AD73B');

UPDATE individual_services AS s
SET individual_service_filter_id = f.id
FROM individual_service_filters AS f
WHERE f.name_en = 'Brakes'
  AND s.code IN ('9D6E0A3A', 'D01DCEC5');

UPDATE individual_services AS s
SET individual_service_filter_id = f.id
FROM individual_service_filters AS f
WHERE f.name_en = 'Fluids'
  AND s.code IN ('E30BF6A7', '510960CB', 'AC34C09A', '7F36DC94', '0C375A5C', 'D73D169B', '6BE988EB');

UPDATE individual_services AS s
SET individual_service_filter_id = f.id
FROM individual_service_filters AS f
WHERE f.name_en = 'Tyres'
  AND s.code IN ('84E1608A', '1E7E352B', 'E07CA117');

UPDATE individual_services AS s
SET individual_service_filter_id = f.id
FROM individual_service_filters AS f
WHERE f.name_en = 'Ignition'
  AND s.code IN ('9BAE070B', '97B707A4', '1A2C1E03');

UPDATE individual_services AS s
SET individual_service_filter_id = f.id
FROM individual_service_filters AS f
WHERE f.name_en = 'Engine'
  AND s.code IN ('E435654D');

UPDATE individual_services AS s
SET individual_service_filter_id = f.id
FROM individual_service_filters AS f
WHERE f.name_en = 'Electrical'
  AND s.code IN ('2A09F8E4');

UPDATE individual_services AS s
SET individual_service_filter_id = f.id
FROM individual_service_filters AS f
WHERE f.name_en = 'Gas system'
  AND s.code IN ('4311BE81', '3864781E', '35BAB181');

-- Boş kalan satır kalmamalı.
SELECT s.id, s.code, s.title_json
FROM individual_services s
WHERE s.individual_service_filter_id IS NULL
ORDER BY s.sort_order;
