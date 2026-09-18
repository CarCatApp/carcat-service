-- Percentage (maintenance item) icons. Hibernate ddl-auto=update also creates these.
-- Redis: photo:percentage:{serviceId} and photo:percentage:empty — DEL on upload, do not overwrite.

CREATE TABLE IF NOT EXISTS percentage_photos (
    image_id    bigserial PRIMARY KEY,
    service_id  bigint NOT NULL,
    file_name   varchar(255),
    file_type   varchar(64),
    image_data  bytea
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_percentage_photos_service_id ON percentage_photos (service_id);

CREATE TABLE IF NOT EXISTS percentage_empty_photos (
    image_id    bigserial PRIMARY KEY,
    file_name   varchar(255),
    file_type   varchar(64),
    image_data  bytea
);
