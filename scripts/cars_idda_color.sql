-- ddl-auto=update adds idda_color and does not drop color_name_text. Not run on boot.
ALTER TABLE cars DROP COLUMN IF EXISTS color_name_text;
ALTER TABLE cars ADD COLUMN IF NOT EXISTS idda_color varchar(80);
