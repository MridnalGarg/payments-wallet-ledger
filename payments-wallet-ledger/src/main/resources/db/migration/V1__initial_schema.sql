CREATE TABLE users (
    user_id         BIGINT PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    email           VARCHAR(320) NOT NULL,
    phone           VARCHAR(30) NOT NULL,
    kyc_status      VARCHAR(30) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone),

    CONSTRAINT chk_users_kyc_status
        CHECK (kyc_status IN (
            'PENDING',
            'VERIFIED',
            'REJECTED',
            'SUSPENDED'
        ))
);


CREATE TABLE accounts (
    account_id      BIGINT PRIMARY KEY,
    account_type    VARCHAR(30) NOT NULL,
    user_id         BIGINT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_accounts_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id),

    CONSTRAINT chk_accounts_type
        CHECK (account_type IN (
            'WALLET',
            'CASH_IN',
            'CASH_OUT',
            'FEE_REVENUE'
        )),

    CONSTRAINT chk_accounts_status
        CHECK (status IN (
            'ACTIVE',
            'SUSPENDED',
            'CLOSED'
        )),

    CONSTRAINT chk_system_account_user
        CHECK (
            (account_type = 'WALLET' AND user_id IS NOT NULL)
            OR
            (account_type <> 'WALLET' AND user_id IS NULL)
        )
);

CREATE TABLE wallets (
    wallet_id       BIGINT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    account_id      BIGINT NOT NULL,
    name            VARCHAR(100) NOT NULL,
    balance         NUMERIC(20,2) NOT NULL DEFAULT 0,
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_wallets_user
        FOREIGN KEY (user_id)
        REFERENCES users (user_id),

    CONSTRAINT fk_wallets_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(account_id),

    CONSTRAINT uq_wallets_account
        UNIQUE (account_id),

    CONSTRAINT chk_wallets_balance
        CHECK (balance >= 0),

    CONSTRAINT chk_wallets_status
        CHECK (status IN (
            'ACTIVE',
            'SUSPENDED',
            'CLOSED'
        ))
);

CREATE TABLE transfers (
    transfer_id             BIGINT PRIMARY KEY,
    type                    VARCHAR(30) NOT NULL,
    status                  VARCHAR(30) NOT NULL,
    source_account_id       BIGINT NOT NULL,
    destination_account_id  BIGINT NOT NULL,
    amount                  NUMERIC(20,2) NOT NULL,
    original_transfer_id    BIGINT NULL,
    fraud_status            VARCHAR(30) NOT NULL DEFAULT 'NOT_CHECKED',
    fraud_reason            VARCHAR(500) NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at            TIMESTAMP NULL,

    CONSTRAINT fk_transfers_source
        FOREIGN KEY (source_account_id)
        REFERENCES accounts(account_id),

    CONSTRAINT fk_transfers_destination
        FOREIGN KEY (destination_account_id)
        REFERENCES accounts(account_id),

    CONSTRAINT fk_transfers_original
        FOREIGN KEY (original_transfer_id)
        REFERENCES transfers(transfer_id),

    CONSTRAINT chk_transfers_amount
        CHECK (amount > 0),

    CONSTRAINT chk_transfers_type
        CHECK (type IN (
            'TOP_UP',
            'PEER_TRANSFER',
            'WITHDRAWAL',
            'FEE',
            'REVERSAL'
        )),

    CONSTRAINT chk_transfers_status
        CHECK (status IN (
            'PENDING',
            'HELD',
            'COMPLETED',
            'FAILED',
            'REVERSED'
        )),

    CONSTRAINT chk_fraud_status
        CHECK (fraud_status IN (
            'NOT_CHECKED',
            'CLEAR',
            'FLAGGED',
            'HELD'
        )),

    CONSTRAINT chk_transfer_not_self
        CHECK (
            source_account_id <> destination_account_id
        )
);

CREATE TABLE ledger_entries (
    id              BIGINT PRIMARY KEY,
    transfer_id     BIGINT NOT NULL,
    account_id      BIGINT NOT NULL,
    direction       VARCHAR(10) NOT NULL,
    amount          NUMERIC(20,2) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_ledger_transfer
        FOREIGN KEY (transfer_id)
        REFERENCES transfers(transfer_id),

    CONSTRAINT fk_ledger_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(account_id),

    CONSTRAINT chk_ledger_direction
        CHECK (direction IN (
            'DEBIT',
            'CREDIT'
        )),

    CONSTRAINT chk_ledger_amount
        CHECK (amount > 0)
);

CREATE TABLE transaction_tokens (
    id                  BIGINT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    transaction_token   VARCHAR(255) NOT NULL,
    request_hash        CHAR(64) NOT NULL,
    response_status     INTEGER NOT NULL,
    response_body       JSONB NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_transaction_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id),

    CONSTRAINT uq_transaction_user_token
        UNIQUE (user_id, transaction_token)
);

CREATE TABLE transfer_status_audits (
    id              BIGINT PRIMARY KEY,
    transfer_id     BIGINT NOT NULL,
    old_status      VARCHAR(30),
    new_status      VARCHAR(30) NOT NULL,
    actor_type      VARCHAR(30) NOT NULL,
    actor_id        BIGINT NULL,
    reason          VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_status_audit_transfer
        FOREIGN KEY (transfer_id)
        REFERENCES transfers(transfer_id),

    CONSTRAINT chk_status_audit_actor
        CHECK (actor_type IN (
            'USER',
            'ADMIN',
            'SYSTEM'
        ))
)

