CREATE TABLE users (
    user_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    email           VARCHAR(320) NOT NULL,
    phone           VARCHAR(30) NOT NULL,
    kyc_status      VARCHAR(30) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE accounts (
    account_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_type    VARCHAR(30) NOT NULL,
    user_id         BIGINT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE wallets (
    wallet_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT DEFAULT NULL,
    account_id      BIGINT DEFAULT NULL,
    name            VARCHAR(100) NOT NULL,
    balance         NUMERIC(20,2) NOT NULL DEFAULT 0,
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE transfers (
    transfer_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type                    VARCHAR(30) NOT NULL,
    status                  VARCHAR(30) NOT NULL,
    source_account_id       BIGINT NOT NULL,
    destination_account_id  BIGINT NOT NULL,
    amount                  NUMERIC(20,2) NOT NULL,
    original_transfer_id    BIGINT NULL,
    fraud_status            VARCHAR(30) NOT NULL DEFAULT 'NOT_CHECKED',
    fraud_reason            VARCHAR(500) NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at            TIMESTAMP NULL
);

CREATE TABLE ledger_entries (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transfer_id     BIGINT NOT NULL,
    account_id      BIGINT NOT NULL,
    direction       VARCHAR(10) NOT NULL,
    amount          NUMERIC(20,2) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE transaction_tokens (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    transaction_token   VARCHAR(255) NOT NULL,
    request_hash        CHAR(64) NOT NULL,
    response_status     INTEGER NOT NULL,
    response_body       JSONB NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE transfer_status_audits (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transfer_id     BIGINT NOT NULL,
    old_status      VARCHAR(30),
    new_status      VARCHAR(30) NOT NULL,
    actor_type      VARCHAR(30) NOT NULL,
    actor_id        BIGINT NULL,
    reason          VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

