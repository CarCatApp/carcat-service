-- Promo codes, accepting branches, and the code a customer accepted.
-- Hibernate ddl-auto=update also creates these. Not run on boot.

CREATE TABLE IF NOT EXISTS promos (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    code VARCHAR(64) NOT NULL,
    code_key VARCHAR(64) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    discount_type VARCHAR(16) NOT NULL,
    discount_value NUMERIC(12, 2) NOT NULL,
    status VARCHAR(16) NOT NULL,
    expired BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS promo_branches (
    id BIGSERIAL PRIMARY KEY,
    promo_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    CONSTRAINT uk_promo_branch UNIQUE (promo_id, branch_id)
);

ALTER TABLE customers ADD COLUMN IF NOT EXISTS promo_id BIGINT;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS promo_code VARCHAR(64);
