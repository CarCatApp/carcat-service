-- Safe if ddl-auto already added the column.
ALTER TABLE customers ADD COLUMN IF NOT EXISTS gender VARCHAR(16);
