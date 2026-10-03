-- Deploy sonrası elle çalıştır. Boot'ta çalışmaz.
-- Hibernate ddl-auto=update individual_service_filters tablosunu açmış olmalı.

INSERT INTO individual_service_filters (id, name) VALUES
    (1, 'Filters'),
    (2, 'Brakes'),
    (3, 'Fluids'),
    (4, 'Tyres'),
    (5, 'Ignition'),
    (6, 'Engine'),
    (7, 'Electrical')
ON CONFLICT (id) DO NOTHING;

SELECT setval(
    pg_get_serial_sequence('individual_service_filters', 'id'),
    (SELECT MAX(id) FROM individual_service_filters)
);
