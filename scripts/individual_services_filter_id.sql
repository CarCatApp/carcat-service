-- Deploy ve individual_service_filters insert'inden sonra elle çalıştır. Boot'ta çalışmaz.
-- Mevcut katalog satırlarına individual_service_filter_id yazar.

UPDATE individual_services AS s
SET individual_service_filter_id = f.id
FROM individual_service_filters AS f
WHERE f.name = 'Filters'
  AND s.code IN ('HVF', 'SLF', 'YNF');

UPDATE individual_services AS s
SET individual_service_filter_id = f.id
FROM individual_service_filters AS f
WHERE f.name = 'Brakes'
  AND s.code IN ('EBD', 'EMD');

UPDATE individual_services AS s
SET individual_service_filter_id = f.id
FROM individual_service_filters AS f
WHERE f.name = 'Fluids'
  AND s.code IN ('MYF', 'ANT', 'SGM', 'TRY');

UPDATE individual_services AS s
SET individual_service_filter_id = f.id
FROM individual_service_filters AS f
WHERE f.name = 'Tyres'
  AND s.code IN ('TBY', 'RZV', 'TKD');

UPDATE individual_services AS s
SET individual_service_filter_id = f.id
FROM individual_service_filters AS f
WHERE f.name = 'Ignition'
  AND s.code IN ('ASD');

UPDATE individual_services AS s
SET individual_service_filter_id = f.id
FROM individual_service_filters AS f
WHERE f.name = 'Engine'
  AND s.code IN ('KMR');

UPDATE individual_services AS s
SET individual_service_filter_id = f.id
FROM individual_service_filters AS f
WHERE f.name = 'Electrical'
  AND s.code IN ('AKM');
