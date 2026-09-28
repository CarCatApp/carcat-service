-- Routine-care groups and service lines. Hibernate ddl-auto=update also creates the tables.
-- Re-runnable: behaviors conflict on code; service lines skip when the same az title already exists in that group.
-- en/ru are empty so admin can fill them. The panel falls back to az until then.

CREATE TABLE IF NOT EXISTS service_behaviors (
    id          bigserial PRIMARY KEY,
    code        varchar(64)   NOT NULL,
    title_json  varchar(1024) NOT NULL,
    sort_order  integer       NOT NULL DEFAULT 0,
    active      boolean       NOT NULL DEFAULT true,
    CONSTRAINT uk_service_behaviors_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS offered_services (
    id          bigserial PRIMARY KEY,
    behavior_id bigint        NOT NULL,
    title_json  varchar(1024) NOT NULL,
    sort_order  integer       NOT NULL DEFAULT 0,
    active      boolean       NOT NULL DEFAULT true
);

INSERT INTO service_behaviors (code, title_json, sort_order, active) VALUES
    ('replace', '{"az":"DƏYİŞDİRMƏ","en":"Replace","ru":""}', 1, true),
    ('extra',   '{"az":"ƏLAVƏ ETMƏ","en":"Top up","ru":""}', 2, true),
    ('inspect', '{"az":"YOXLANIŞ VƏ DİAQNOZ","en":"Check & diagnose","ru":""}', 3, true),
    ('service', '{"az":"Servis göstəricisi","en":"Service indicator","ru":""}', 4, true)
ON CONFLICT (code) DO NOTHING;

INSERT INTO offered_services (behavior_id, title_json, sort_order, active)
SELECT b.id, v.title_json, v.sort_order, true
FROM service_behaviors b
JOIN (VALUES
    ('replace', 1,  '{"az":"Mühərrik yağı","en":"","ru":""}'),
    ('replace', 2,  '{"az":"Mühərrikin yağ filtri","en":"","ru":""}'),
    ('replace', 3,  '{"az":"Hava filtri","en":"","ru":""}'),
    ('replace', 4,  '{"az":"Yanacaq Filtri","en":"","ru":""}'),
    ('replace', 5,  '{"az":"Salon (kabin) filtri","en":"","ru":""}'),
    ('extra', 1,    '{"az":"Şüşəyuyan mayesi","en":"","ru":""}'),
    ('extra', 2,    '{"az":"Sükan gücləndirici mayesi","en":"","ru":""}'),
    ('extra', 3,    '{"az":"Soyutma mayesi (Antifriz)","en":"","ru":""}'),
    ('extra', 4,    '{"az":"Əyləc mayesi","en":"","ru":""}'),
    ('extra', 5,    '{"az":"Akkumulyator mayesi","en":"","ru":""}'),
    ('inspect', 1,  '{"az":"AdBlue","en":"","ru":""}'),
    ('inspect', 2,  '{"az":"Əyləc diskləri","en":"","ru":""}'),
    ('inspect', 3,  '{"az":"Əyləc boruları və şlanqları","en":"","ru":""}'),
    ('inspect', 4,  '{"az":"Əyləc supportları","en":"","ru":""}'),
    ('inspect', 5,  '{"az":"Baraban əyləcləri","en":"","ru":""}'),
    ('inspect', 6,  '{"az":"Əyləc Qəlibləri","en":"","ru":""}'),
    ('inspect', 7,  '{"az":"Əl əyləci","en":"","ru":""}'),
    ('inspect', 8,  '{"az":"Əyləc mayesi","en":"","ru":""}'),
    ('inspect', 9,  '{"az":"Paylayıcı qayış","en":"","ru":""}'),
    ('inspect', 10, '{"az":"İlişmə Muftası","en":"","ru":""}'),
    ('inspect', 11, '{"az":"Köməkçi qayışlar","en":"","ru":""}'),
    ('inspect', 12, '{"az":"Mühərrik yağı sızmaları","en":"","ru":""}'),
    ('inspect', 13, '{"az":"Radiator və antifriz şlanqları","en":"","ru":""}'),
    ('inspect', 14, '{"az":"Mühərrikin performansı","en":"","ru":""}'),
    ('inspect', 15, '{"az":"Yanacaq sistemi və çəni","en":"","ru":""}'),
    ('inspect', 16, '{"az":"Güzgülər","en":"","ru":""}'),
    ('inspect', 17, '{"az":"Yanacaq çəninin qapağı","en":"","ru":""}'),
    ('inspect', 18, '{"az":"Qapı, kapot və kilidlərin işləməsi","en":"","ru":""}'),
    ('inspect', 19, '{"az":"Siqnal","en":"","ru":""}'),
    ('inspect', 20, '{"az":"Kondisioner","en":"","ru":""}'),
    ('inspect', 21, '{"az":"Amortizatorlar","en":"","ru":""}'),
    ('inspect', 22, '{"az":"Təkər (podşipniklər)","en":"","ru":""}'),
    ('inspect', 23, '{"az":"Sükan reykası və hissələri","en":"","ru":""}'),
    ('inspect', 24, '{"az":"Təkərlərin vəziyyəti və protektoru","en":"","ru":""}'),
    ('inspect', 25, '{"az":"Təkər təzyiqi","en":"","ru":""}'),
    ('inspect', 26, '{"az":"Təkər boltlarının yoxlanışı","en":"","ru":""}'),
    ('inspect', 27, '{"az":"Egzozda tüstü yoxlanışı","en":"","ru":""}'),
    ('inspect', 28, '{"az":"Egzoz sistemi","en":"","ru":""}'),
    ('inspect', 29, '{"az":"Transmissiya Mayesi","en":"","ru":""}'),
    ('inspect', 30, '{"az":"Generator Qayışı","en":"","ru":""}')
) AS v(code, sort_order, title_json) ON v.code = b.code
WHERE NOT EXISTS (
    SELECT 1 FROM offered_services o
    WHERE o.behavior_id = b.id
      AND o.title_json::jsonb ->> 'az' = v.title_json::jsonb ->> 'az'
);
