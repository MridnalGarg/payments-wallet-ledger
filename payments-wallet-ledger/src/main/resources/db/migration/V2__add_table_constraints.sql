
ALTER TABLE users
    ADD CONSTRAINT uq_users_email UNIQUE (email),

    ADD CONSTRAINT uq_users_phone UNIQUE (phone),

    ADD CONSTRAINT chk_users_kyc_status
        CHECK (kyc_status IN (
            'PENDING',
            'VERIFIED',
            'REJECTED',
            'SUSPENDED'
        ));

ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id),

    ADD CONSTRAINT chk_accounts_type
        CHECK (account_type IN (
            'WALLET',
            'CASH_IN',
            'CASH_OUT',
            'FEE_REVENUE'
        )),

    ADD CONSTRAINT chk_accounts_status
        CHECK (status IN (
            'ACTIVE',
            'FROZEN',
            'SUSPENDED'
            'CLOSED'
        )),

    ADD CONSTRAINT chk_system_account_user
        CHECK (
            (account_type = 'WALLET' AND user_id IS NOT NULL)
            OR
            (account_type <> 'WALLET' AND user_id IS NULL)
        );

ALTER TABLE wallets

    ADD CONSTRAINT fk_wallets_user
        FOREIGN KEY (user_id)
        REFERENCES users (user_id),

    ADD CONSTRAINT fk_wallets_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(account_id),

    ADD CONSTRAINT uq_wallets_account
        UNIQUE (account_id),

    ADD CONSTRAINT chk_wallets_balance
        CHECK (balance >= 0),

    ADD CONSTRAINT chk_wallets_status
        CHECK (status IN (
            'ACTIVE',
            'FROZEN'
            'SUSPENDED',
            'CLOSED'
        ));

ALTER TABLE transfers
    ADD CONSTRAINT fk_transfers_source
        FOREIGN KEY (source_account_id)
        REFERENCES accounts(account_id),

    ADD CONSTRAINT fk_transfers_destination
        FOREIGN KEY (destination_account_id)
        REFERENCES accounts(account_id),

    ADD CONSTRAINT fk_transfers_original
        FOREIGN KEY (original_transfer_id)
        REFERENCES transfers(transfer_id),

    ADD CONSTRAINT chk_transfers_amount
        CHECK (amount > 0),

    ADD CONSTRAINT chk_transfers_type
        CHECK (type IN (
            'TOP_UP',
            'PEER_TRANSFER',
            'WITHDRAWAL',
            'FEE',
            'REVERSAL'
        )),

    ADD CONSTRAINT chk_transfers_status
        CHECK (status IN (
            'PENDING',
            'HELD',
            'COMPLETED',
            'FAILED',
            'REVERSED'
        )),

    ADD CONSTRAINT chk_fraud_status
        CHECK (fraud_status IN (
            'NOT_CHECKED',
            'CLEAR',
            'FLAGGED',
            'HELD'
        )),

    ADD CONSTRAINT chk_transfer_not_self
        CHECK (
        source_account_id <> destination_account_id
        );

ALTER TABLE ledger_entries
    ADD CONSTRAINT fk_ledger_transfer
        FOREIGN KEY (transfer_id)
        REFERENCES transfers(transfer_id),

    ADD CONSTRAINT fk_ledger_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(account_id),

    ADD CONSTRAINT chk_ledger_direction
        CHECK (direction IN (
            'DEBIT',
            'CREDIT'
        )),

    ADD CONSTRAINT chk_ledger_amount
        CHECK (amount > 0);

ALTER TABLE transaction_tokens
    ADD CONSTRAINT fk_transaction_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id),

    ADD CONSTRAINT uq_transaction_user_token
        UNIQUE (user_id, transaction_token);

ALTER TABLE transfer_status_audits
    ADD CONSTRAINT fk_status_audit_transfer
        FOREIGN KEY (transfer_id)
        REFERENCES transfers(transfer_id),

    ADD CONSTRAINT chk_status_audit_actor
        CHECK (actor_type IN (
            'USER',
            'ADMIN',
            'SYSTEM'
        ));