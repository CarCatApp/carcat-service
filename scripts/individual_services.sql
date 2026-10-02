-- Shared individual-service catalog. Hibernate ddl-auto=update also creates the tables.
-- Branch prices live in branch_individual_services and are not seeded.
-- Re-runnable: a code that already exists is left unchanged.

CREATE TABLE IF NOT EXISTS individual_services (
    id          bigserial PRIMARY KEY,
    code        varchar(8)    NOT NULL,
    title_json  varchar(1024) NOT NULL,
    sort_order  integer       NOT NULL DEFAULT 0,
    active      boolean       NOT NULL DEFAULT true,
    CONSTRAINT uk_individual_services_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS branch_individual_services (
    id                      bigserial PRIMARY KEY,
    branch_id               bigint  NOT NULL,
    individual_service_id   bigint  NOT NULL,
    active                  boolean NOT NULL DEFAULT false,
    price_simple            integer,
    price_medium            integer,
    price_complex           integer,
    CONSTRAINT uk_branch_individual_service UNIQUE (branch_id, individual_service_id)
);

INSERT INTO individual_services (code, title_json, sort_order, active) VALUES
    ('MYF', '{"az":"Mühərrik yağı və filtri dəyişimi","en":"Engine oil and filter change","ru":"Замена моторного масла и фильтра"}', 1, true),
    ('HVF', '{"az":"Hava filtri dəyişimi","en":"Air filter change","ru":"Замена воздушного фильтра"}', 2, true),
    ('SLF', '{"az":"Salon filtri dəyişimi","en":"Cabin filter change","ru":"Замена салонного фильтра"}', 3, true),
    ('TBY', '{"az":"Təkərlərin balansı və yerdəyişməsi","en":"Wheel balance and rotation","ru":"Балансировка и перестановка шин"}', 4, true),
    ('RZV', '{"az":"Razval","en":"Wheel alignment","ru":"Развал-схождение"}', 5, true),
    ('EBD', '{"az":"Əyləc bəndləri dəyişimi","en":"Brake pad change","ru":"Замена тормозных колодок"}', 6, true),
    ('EMD', '{"az":"Əyləc mayesinin dəyişilməsi","en":"Brake fluid change","ru":"Замена тормозной жидкости"}', 7, true),
    ('YNF', '{"az":"Yanacaq filtri dəyişimi","en":"Fuel filter change","ru":"Замена топливного фильтра"}', 8, true),
    ('ASD', '{"az":"Alışdırma şamları dəyişimi","en":"Spark plug change","ru":"Замена свечей зажигания"}', 9, true),
    ('ANT', '{"az":"Soyutma mayesi (antifriz) dəyişimi","en":"Coolant (antifreeze) change","ru":"Замена охлаждающей жидкости (антифриз)"}', 10, true),
    ('SGM', '{"az":"Sükan gücləndirici mayesi dəyişimi","en":"Power steering fluid change","ru":"Замена жидкости гидроусилителя руля"}', 11, true),
    ('TRY', '{"az":"Transmissiya yağı dəyişimi","en":"Transmission oil change","ru":"Замена трансмиссионного масла"}', 12, true),
    ('KMR', '{"az":"Kəmər dəyişimi","en":"Belt change","ru":"Замена ремня"}', 13, true),
    ('AKM', '{"az":"Akkumulyator dəyişimi","en":"Battery change","ru":"Замена аккумулятора"}', 14, true),
    ('TKD', '{"az":"Təkərlərin dəyişimi","en":"Tire change","ru":"Замена шин"}', 15, true)
ON CONFLICT (code) DO NOTHING;
