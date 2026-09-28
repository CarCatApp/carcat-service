-- Service categories for the partner panel "Xidmetler" cards.
-- Hibernate ddl-auto=update also creates these tables.
-- Redis: photo:service-category:{categoryId}:bytes and :type — DEL on icon upload/delete. Do not overwrite.

CREATE TABLE IF NOT EXISTS service_categories (
    id                bigserial PRIMARY KEY,
    code              varchar(64)  NOT NULL,
    title_json        varchar(1024) NOT NULL,
    description_json  varchar(2048),
    sort_order        integer      NOT NULL DEFAULT 0,
    openable          boolean      NOT NULL DEFAULT false,
    toggleable        boolean      NOT NULL DEFAULT true,
    active            boolean      NOT NULL DEFAULT true,
    direction_keys    varchar(256),
    CONSTRAINT uk_service_categories_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS branch_service_categories (
    id           bigserial PRIMARY KEY,
    branch_id    bigint  NOT NULL,
    category_id  bigint  NOT NULL,
    active       boolean NOT NULL,
    CONSTRAINT uk_branch_service_categories_branch_category UNIQUE (branch_id, category_id)
);

CREATE TABLE IF NOT EXISTS service_category_photos (
    image_id     bigserial PRIMARY KEY,
    category_id  bigint NOT NULL,
    file_name    varchar(255),
    file_type    varchar(255),
    image_data   bytea,
    CONSTRAINT uk_service_category_photos_category_id UNIQUE (category_id)
);

INSERT INTO service_categories (
    code, title_json, description_json, sort_order, openable, toggleable, active, direction_keys
) VALUES (
    'routine',
    '{"az":"Dövri Qulluq","en":"Routine Care","ru":"Плановое обслуживание"}',
    '{"az":"Paketlər və fərdi dövri qulluq xidmətləri","en":"Packages and individual routine care services","ru":"Пакеты и отдельные услуги планового обслуживания"}',
    1, true, false, true, NULL
) ON CONFLICT (code) DO NOTHING;

INSERT INTO service_categories (
    code, title_json, description_json, sort_order, openable, toggleable, active, direction_keys
) VALUES (
    'repair_inspection',
    '{"az":"Təmir Xidməti və Yoxlanış","en":"Repair & Inspection","ru":"Ремонт и диагностика"}',
    '{"az":"Avtomobil təmir və yoxlanış xidmətləri","en":"Vehicle repair and inspection services","ru":"Ремонт и диагностика автомобиля"}',
    2, false, true, true, 'dir:repair,dir:inspection'
) ON CONFLICT (code) DO NOTHING;
