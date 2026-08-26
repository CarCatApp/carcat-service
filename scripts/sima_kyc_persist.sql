-- SIMA KYC persist (CRCT-248 / CRCT-249)
-- Safe if ddl-auto already added the objects.

ALTER TABLE customers ADD COLUMN IF NOT EXISTS sima_verified boolean NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS sima_kyc_records (
    id                          bigserial PRIMARY KEY,
    customer_id                 bigint NOT NULL REFERENCES customers (user_id),
    channel                     varchar(16) NOT NULL,
    verified                    boolean NOT NULL,
    applied_to_profile          boolean NOT NULL,
    idempotency_key             varchar(64) NOT NULL,
    transaction_id              varchar(255),
    process_time                varchar(255),
    pin                         varchar(255),
    document_number             varchar(255),
    name                        varchar(255),
    surname                     varchar(255),
    patronymic                  varchar(255),
    birth_date                  varchar(255),
    birth_address               varchar(255),
    address                     varchar(255),
    nationality                 varchar(255),
    gender                      varchar(255),
    exp_date                    varchar(255),
    document_type               varchar(255),
    issuing_country             varchar(255),
    liveness_score              double precision,
    liveness_status             boolean,
    liveness_failure_reason     varchar(255),
    similarity_score            double precision,
    similarity_status           boolean,
    sima_http_status            integer,
    sima_response_code          integer,
    sima_message                varchar(1000),
    outcome                     varchar(64) NOT NULL,
    created_at                  timestamp NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sima_kyc_idempotency ON sima_kyc_records (idempotency_key);
CREATE INDEX IF NOT EXISTS idx_sima_kyc_customer ON sima_kyc_records (customer_id);
