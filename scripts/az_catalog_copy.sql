-- Manual. Does not run on boot.
-- Azerbaijani catalog copy. Idempotent: each UPDATE matches the old text only.
BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM individual_services
        WHERE code IN ('1E84A87D', '5C925F3C', '558076BA')
          AND id NOT IN (27, 30, 32)
    ) THEN
        RAISE EXCEPTION 'individual service code already used';
    END IF;
END $$;

UPDATE service_behaviors
SET title_json = jsonb_set(title_json::jsonb, '{az}', to_jsonb('Dəyişdirmə'::text))::text
WHERE code = 'replace'
  AND title_json::jsonb ->> 'az' = 'DƏYİŞDİRMƏ';

UPDATE service_behaviors
SET title_json = jsonb_set(title_json::jsonb, '{az}', to_jsonb('Əlavə etmə'::text))::text
WHERE code = 'extra'
  AND title_json::jsonb ->> 'az' = 'ƏLAVƏ ETMƏ';

UPDATE service_categories
SET title_json = jsonb_set(title_json::jsonb, '{az}', to_jsonb('Dövri qulluq'::text))::text
WHERE code = 'routine'
  AND title_json::jsonb ->> 'az' = 'Dövri Qulluq';

UPDATE offered_services
SET title_json = jsonb_set(title_json::jsonb, '{az}', to_jsonb('Yanacaq filtri'::text))::text
WHERE title_json::jsonb ->> 'az' = 'Yanacaq Filtri';

UPDATE offered_services
SET title_json = jsonb_set(title_json::jsonb, '{az}', to_jsonb('Sükan gücləndiricisi mayesi'::text))::text
WHERE title_json::jsonb ->> 'az' = 'Sükan gücləndirici mayesi';

UPDATE offered_services
SET title_json = jsonb_set(title_json::jsonb, '{az}', to_jsonb('Soyutma mayesi (antifriz)'::text))::text
WHERE title_json::jsonb ->> 'az' = 'Soyutma mayesi (Antifriz)';

UPDATE services
SET name_az = 'Salon (kabin) filtri'
WHERE name_az = 'Salon filtri';

UPDATE services
SET name_az = 'Təkərlərin düzlənməsi (razval)'
WHERE name_az = 'Razval';

UPDATE services
SET name_az = 'Sükan gücləndiricisi mayesi'
WHERE name_az = 'Sükan gücləndirici mayesi';

UPDATE services
SET name_az = 'İnverterin soyutma mayesi (antifriz)'
WHERE name_az = 'Inverterin Soyutma mayesi (antifriz)';

UPDATE individual_services
SET title_json = jsonb_set(title_json::jsonb, '{az}', to_jsonb('Təkərlərin düzlənməsi (razval)'::text))::text,
    code = '1E84A87D'
WHERE id = 27
  AND title_json::jsonb ->> 'az' = 'Razval';

UPDATE individual_services
SET title_json = jsonb_set(title_json::jsonb, '{az}', to_jsonb('Salon (kabin) filtri'::text))::text,
    code = '5C925F3C'
WHERE id = 30
  AND title_json::jsonb ->> 'az' = 'Salon filtri';

UPDATE individual_services
SET title_json = jsonb_set(title_json::jsonb, '{az}', to_jsonb('Sükan gücləndiricisi mayesi'::text))::text,
    code = '558076BA'
WHERE id = 32
  AND title_json::jsonb ->> 'az' = 'Sükan gücləndirici mayesi';

UPDATE individual_services
SET title_json = jsonb_set(title_json::jsonb, '{az}', to_jsonb('İnverterin soyutma mayesi (antifriz)'::text))::text
WHERE id = 20
  AND title_json::jsonb ->> 'az' = 'Inverterin Soyutma mayesi (antifriz)';

UPDATE booking_individual_lines
SET title_json = jsonb_set(title_json::jsonb, '{az}', to_jsonb('Təkərlərin düzlənməsi (razval)'::text))::text
WHERE title_json::jsonb ->> 'az' = 'Razval';

UPDATE booking_individual_lines
SET title_json = jsonb_set(title_json::jsonb, '{az}', to_jsonb('Salon (kabin) filtri'::text))::text
WHERE title_json::jsonb ->> 'az' = 'Salon filtri';

UPDATE booking_individual_lines
SET title_json = jsonb_set(title_json::jsonb, '{az}', to_jsonb('Sükan gücləndiricisi mayesi'::text))::text
WHERE title_json::jsonb ->> 'az' = 'Sükan gücləndirici mayesi';

UPDATE booking_individual_lines
SET title_json = jsonb_set(title_json::jsonb, '{az}', to_jsonb('İnverterin soyutma mayesi (antifriz)'::text))::text
WHERE title_json::jsonb ->> 'az' = 'Inverterin Soyutma mayesi (antifriz)';

COMMIT;

SELECT 'behavior' AS src, code, title_json::jsonb ->> 'az' AS az FROM service_behaviors WHERE code IN ('replace', 'extra');
SELECT 'category' AS src, code, title_json::jsonb ->> 'az' AS az FROM service_categories WHERE code = 'routine';
SELECT 'offered' AS src, id::text, title_json::jsonb ->> 'az' AS az FROM offered_services WHERE id IN (4, 5, 7, 8) ORDER BY id;
SELECT 'individual' AS src, id::text, code, title_json::jsonb ->> 'az' AS az FROM individual_services WHERE id IN (20, 27, 30, 32) ORDER BY id;
SELECT 'services_left' AS src, name_az, count(*) FROM services
WHERE name_az IN (
    'Razval', 'Salon filtri', 'Sükan gücləndirici mayesi', 'Inverterin Soyutma mayesi (antifriz)',
    'Təkərlərin düzlənməsi (razval)', 'Salon (kabin) filtri', 'Sükan gücləndiricisi mayesi',
    'İnverterin soyutma mayesi (antifriz)'
)
GROUP BY name_az
ORDER BY name_az;
