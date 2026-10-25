-- Safe if ddl-auto already added the column. Not run on boot.
ALTER TABLE cars ADD COLUMN IF NOT EXISTS color_name_text varchar(80);
