-- Fresh install. Boot'ta çalışmaz.
-- Mevcut tablo (name kolonu) için individual_service_filters_i18n.sql kullan.

INSERT INTO individual_service_filters (id, name_az, name_en, name_ru) VALUES
    (1, 'Filtrlər', 'Filters', 'Фильтры'),
    (2, 'Əyləclər', 'Brakes', 'Тормоза'),
    (3, 'Mayelər', 'Fluids', 'Жидкости'),
    (4, 'Təkərlər', 'Tyres', 'Шины'),
    (5, 'Alışdırma', 'Ignition', 'Зажигание'),
    (6, 'Mühərrik', 'Engine', 'Двигатель'),
    (7, 'Elektrik', 'Electrical', 'Электрика')
ON CONFLICT (id) DO NOTHING;

SELECT setval(
    pg_get_serial_sequence('individual_service_filters', 'id'),
    (SELECT MAX(id) FROM individual_service_filters)
);
