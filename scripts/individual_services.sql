-- Shared individual-service catalog. Hibernate ddl-auto=update also creates the tables.
-- Branch prices live in branch_individual_services and are not seeded.
-- Rows are filled on boot by IndividualCatalogSync from the services table.
-- Do not insert the old MYF…TKD mock list here.

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

