-- Step 1: add isnew column (safe if already exists via ddl-auto)
ALTER TABLE brands ADD COLUMN IF NOT EXISTS isnew VARCHAR(10);
ALTER TABLE models ADD COLUMN IF NOT EXISTS isnew VARCHAR(10);
