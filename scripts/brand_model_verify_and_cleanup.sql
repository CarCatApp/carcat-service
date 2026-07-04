-- Step 3: verify counts (expected: 218 brands, 2212 models with isnew='.')
SELECT COUNT(*) AS new_brands FROM brands WHERE isnew = '.';
SELECT COUNT(*) AS new_models FROM models WHERE isnew = '.';

SELECT b.brand_name, COUNT(m.model_id) AS model_count
FROM brands b
LEFT JOIN models m ON m.brand_id = b.brand_id AND m.isnew = '.'
WHERE b.isnew = '.'
GROUP BY b.brand_id, b.brand_name
ORDER BY b.brand_name;

-- Step 4: delete old rows ONLY after counts match expected values
-- BEGIN;
-- DELETE FROM models WHERE isnew IS NULL;
-- DELETE FROM brands WHERE isnew IS NULL;
-- SELECT COUNT(*) FROM brands;
-- SELECT COUNT(*) FROM models;
-- COMMIT;
