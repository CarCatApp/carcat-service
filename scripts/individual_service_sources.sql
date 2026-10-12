-- services.id (percentages serviceId) → individual_services.id.
-- IndividualCatalogSync rewrites every row on startup. Do not seed by hand.
-- Hibernate ddl-auto=update also creates the table.

CREATE TABLE IF NOT EXISTS individual_service_sources (
    service_id              bigint PRIMARY KEY,
    individual_service_id   bigint NOT NULL
);
