-- Ensure there is at most one account of each system type.
CREATE UNIQUE INDEX uq_system_accounts_type
    ON accounts (account_type)
    WHERE account_type IN ('CASH_IN', 'CASH_OUT', 'FEE_REVENUE');

-- Create the shared system accounts if they do not already exist.
INSERT INTO accounts (account_type, user_id, status)
VALUES
    ('CASH_IN', NULL, 'ACTIVE'),
    ('CASH_OUT', NULL, 'ACTIVE'),
    ('FEE_REVENUE', NULL, 'ACTIVE')
    ON CONFLICT (account_type)
WHERE account_type IN ('CASH_IN', 'CASH_OUT', 'FEE_REVENUE')
    DO NOTHING;