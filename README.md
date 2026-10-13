# Payments Wallet Ledger

A Spring Boot wallet and ledger application built to explore
financial-system fundamentals: wallet ownership, account modeling,
double-entry bookkeeping, transactional transfers, and database-backed
authentication.

> **Project status:** Core user registration/authentication and
> wallet/transfer flows are in progress. Top-ups are currently intended
> for local development/testing only. This is not production-ready and
> must not be used to hold or move real money.

## Table of Contents

-   [Overview](#overview)
-   [Technology Stack](#technology-stack)
-   [Architecture and Domain Model](#architecture-and-domain-model)
-   [Ledger Rules](#ledger-rules)
-   [Implemented / Current Progress](#implemented--current-progress)
-   [Top-Up and Peer Transfer Flows](#top-up-and-peer-transfer-flows)
-   [System Accounts](#system-accounts)
-   [Security](#security)
-   [Database and Migrations](#database-and-migrations)
-   [Local Development](#local-development)
-   [API Workflows](#api-workflows)
-   [Pending Work](#pending-work)
-   [Production Readiness Checklist](#production-readiness-checklist)

## Overview

The application models user wallets backed by internal accounting
accounts. Transfers are recorded in a ledger using debit and credit
entries. The intended source of truth for wallet balances is the
ledger---not a separately edited `wallets.balance` field.

The current design is a **modular monolith** backed by PostgreSQL. This
keeps transaction boundaries and consistency rules within one
application and one relational database while the core domain is being
developed.

## Technology Stack

-   Java
-   Spring Boot
-   Spring Security with HTTP Basic authentication
-   Spring Data JPA / Hibernate
-   PostgreSQL
-   Flyway database migrations
-   Bean Validation
-   Docker for local PostgreSQL development

Confirm the exact versions from the project's build file before
publishing or deploying.

## Architecture and Domain Model

### Users

Users represent identity and KYC information. The database-backed
authentication flow uses a stored password hash; passwords must never be
stored or logged in plain text.

KYC statuses in the schema include:

-   `PENDING`
-   `VERIFIED`
-   `REJECTED`
-   `SUSPENDED`

Wallet creation is intended to be restricted to users whose KYC status
is `VERIFIED`.

### Wallets

A user may own multiple wallets (for example, a main wallet and a
savings wallet). Each wallet is denominated in INR.

Each wallet is linked to exactly one `WALLET` account. The
wallet-to-account association is unique so that one accounting account
cannot be attached to multiple wallets.

### Accounts

Accounts are internal accounting abstractions, not necessarily real bank
accounts.

  -----------------------------------------------------------------------
  Account type                        Purpose
  ----------------------------------- -----------------------------------
  `WALLET`                            A user's wallet ledger account

  `CASH_IN`                           Platform-side ledger account for
                                      funding/top-up operations

  `CASH_OUT`                          Platform-side ledger account for
                                      withdrawal/payout operations

  `FEE_REVENUE`                       Tracks fees earned by the platform
  -----------------------------------------------------------------------

`WALLET` accounts belong to a user. System accounts (`CASH_IN`,
`CASH_OUT`, `FEE_REVENUE`) are shared platform accounts and have no
associated user.

Account status is separate from account type. Lifecycle statuses include
`ACTIVE`, `FROZEN`, `SUSPENDED`, and `CLOSED`, subject to the
constraints in the active database schema.

### Transfers

Transfer types currently defined/planned in the domain:

-   `TOP_UP`
-   `PEER_TRANSFER`
-   `WITHDRAWAL`
-   `FEE`
-   `REVERSAL`

Transfer statuses include `PENDING`, `HELD`, `COMPLETED`, `FAILED`, and
`REVERSED`.

Fraud status is modeled separately, with values such as `NOT_CHECKED`,
`CLEAR`, `FLAGGED`, and `HELD`.

### Ledger Entries

A ledger entry records:

-   The transfer it belongs to
-   The account affected
-   The direction (`DEBIT` or `CREDIT`)
-   The positive monetary amount
-   The creation timestamp

A completed transfer should produce balanced ledger entries: total
debits must equal total credits for that transfer.

## Ledger Rules

1.  **The ledger is the source of truth.** Calculate an account's ledger
    balance from entries rather than relying on `wallets.balance`.
2.  **Use decimal money types.** Use `BigDecimal` in Java and
    `NUMERIC(20,2)` in PostgreSQL. Reject amounts that are non-positive
    or have more than two decimal places.
3.  **Write atomically.** Persist the transfer, its ledger entries, and
    its final status within one database transaction.
4.  **Validate wallet ownership.** An authenticated user may initiate a
    transfer only from a wallet they own.
5.  **Prevent overspending.** Check available funds for user wallet
    accounts and use a consistent locking strategy to prevent concurrent
    double-spending.
6.  **Do not apply user-wallet spending checks to system accounts.**
    System accounts still require valid account status, balanced
    entries, and reconciliation.
7.  **Do not credit real wallets based on unverified top-up requests.**
    Production top-ups must be confirmed through a trusted
    payment-provider integration.
8.  **Make operations tokenized.** Retries must not create duplicate
    money transfers.

With the current debit/credit convention, an account's displayed balance
is calculated as:

`balance = sum(CREDIT entries) - sum(DEBIT entries)`

This is the project's operational wallet-balance convention. Production
accounting should document normal balances and reconcile ledger activity
against external settlement records.

## Implemented / Current Progress

Based on the current development progress, the following areas have been
worked on. Verify each item against the current branch and test suite
before treating it as complete.

-   [x] PostgreSQL schema for users, accounts, wallets, transfers,
    ledger entries, idempotency tokens, and transfer-status audit
    records has been drafted through Flyway migrations.
-   [x] Domain model distinguishes users, wallets, internal accounts,
    transfers, and ledger entries.
-   [x] Wallet creation is designed to create the wallet and its
    associated account together, subject to KYC verification.
-   [x] Database-backed user authentication has replaced the intended
    in-memory test-user approach.
-   [x] Password hashing uses a Spring Security `PasswordEncoder` with
    BCrypt.
-   [x] A peer-transfer flow has been implemented far enough to validate
    and transfer money using ledger-derived balances.
-   [x] System account types have been defined for `CASH_IN`,
    `CASH_OUT`, and `FEE_REVENUE`.
-   [x] Confirm system accounts are seeded exactly once by a versioned
    Flyway migration in the current branch.
-   [x] Confirm top-up service, controller, and tests compile and run
    together in the current branch.
-   [] Confirm all ledger and transfer changes roll back together on
    any failure.

The checklist reflects the work discussed so far, not an independent
inspection of the current repository.

## Top-Up and Peer Transfer Flows

### Simulated top-up for local development

The intended simulated top-up flow is:

1.  Authenticate the user and verify ownership of the target wallet.
2.  Validate the amount.
3.  Confirm that the wallet and its associated account are active.
4.  Retrieve the active platform `CASH_IN` account.
5.  Lock the relevant account rows using the application's consistent
    account-ID ordering.
6.  Create a `TOP_UP` transfer.
7.  Write two ledger entries:
    -   `DEBIT` the `CASH_IN` account.
    -   `CREDIT` the user's wallet account.
8.  Mark the transfer completed and commit all writes atomically.
9.  Verify the wallet's ledger-derived balance.

This is **simulated funding**, not proof that funds have been received.
Keep it restricted to development/test environments.

Example request body (endpoint path depends on the controller's base
mapping):

``` json
{
  "walletId": 1,
  "amount": 1000.00
}
```

### Peer transfer

The intended peer-transfer flow is:

1.  Authenticate the sender.
2.  Verify that the sender owns the source wallet.
3.  Verify that both wallets and their accounts are active.
4.  Lock both account rows in a consistent order.
5.  Calculate the sender's available balance from ledger entries.
6.  Reject the transfer if the sender's balance is insufficient.
7.  Create a `PEER_TRANSFER` record.
8.  Debit the sender wallet and credit the receiver wallet with equal
    amounts.
9.  Complete the transfer within the same database transaction.

`CASH_IN` is not involved in a peer-to-peer transfer.

## System Accounts

The platform should have one shared account of each system type:

-   `CASH_IN`
-   `CASH_OUT`
-   `FEE_REVENUE`

They should be created once, not per user or per wallet. A Flyway
migration is the preferred way to seed them. A partial unique index on
`accounts.account_type` for these three system types can enforce at most
one account per system type while allowing multiple `WALLET` accounts.

Verify the active schema and existing rows before adding a migration or
index. Flyway migrations that have already run should not be edited; add
a new version instead.

System account balances should not be checked using the same
insufficient-spendable-balance rule as user wallets. However, all
system-account movements must remain balanced, valid, auditable, and
reconcilable.

## Security

-   Public registration should not accept a caller-supplied role; new
    users should default to `USER`.
-   Administrative endpoints should require the appropriate role.
-   Passwords should be hashed before storage and never logged.
-   Authentication should retrieve the stored password hash and let
    Spring Security verify the submitted password.
-   Do not expose `password_hash` in API responses.
-   HTTP Basic is suitable for local API testing. Use HTTPS and an
    appropriate production authentication/session/token strategy outside
    local development.
-   Disabling CSRF is a deliberate API configuration choice and must be
    revisited if browser cookie/session authentication is introduced.
-   Do not expose simulated funding endpoints in production.

## Database and Migrations

Flyway is the schema migration mechanism. Hibernate is configured to
validate the schema rather than generate it automatically.

Guidelines:

-   Use the next unused Flyway version for schema changes.
-   Do not modify migrations that have already been applied to shared
    environments.
-   Verify check constraints and indexes against the live schema.
-   Do not delete the PostgreSQL Docker volume as a routine
    troubleshooting step; that destroys persisted local data.
-   Confirm any manually applied schema changes are captured in a
    migration so a clean environment can be reproduced.

## Local Development

1.  Install a compatible JDK and Docker.
2.  Configure the PostgreSQL connection using the application's
    configuration or environment variables.
3.  Start the local PostgreSQL container using the project's existing
    Docker configuration.
4.  Run the Spring Boot application; Flyway should apply pending
    migrations.
5.  Register a test user.
6.  Complete the required KYC flow and create a wallet.
7.  Ensure the platform system accounts exist.
8.  Use the simulated top-up flow to fund a development wallet.
9.  Test a peer transfer and verify the resulting ledger entries and
    balances.

Do not commit credentials, database passwords, provider secrets, or real
user data. Use environment variables or a local untracked configuration
file for secrets.

## API Workflows

Exact paths and response shapes must be confirmed against the current
controllers.

  -----------------------------------------------------------------------
  Workflow                            Expected behavior
  ----------------------------------- -----------------------------------
  Register user                       Create user and store a BCrypt
                                      password hash

  Authenticate                        Use database-backed credentials

  Get user                            Require authentication

  Verify KYC                          Apply the project's authorization
                                      and KYC rules

  Create wallet                       Require a KYC-verified user; create
                                      wallet and account atomically

  Simulated top-up                    Development only; write balanced
                                      ledger entries

  Peer transfer                       Verify ownership and available
                                      balance; write balanced ledger
                                      entries

  Get balance                         Derive balance from ledger entries

  Withdrawal                          Verify ownership and available
                                      balance; Withdraw amount within balance;
                                      write ledger entries
  -----------------------------------------------------------------------

## Pending Work

The following is the recommended backlog. Some items may already exist
partially in the code; confirm implementation and tests before marking
them done.

### 0 --- Correctness and safety

-   [ ] **Tokenization:** Integrate `transaction_tokens` or an equivalent
    mechanism for top-ups and transfers. Reject reuse of a token with a
    different request payload and return the original result for a
    legitimate retry.
-   [ ] **Concurrency tests:** Prove that concurrent transfers cannot
    spend the same wallet funds twice. Use database row locks
    consistently across every money-moving operation.
-   [ ] **Atomicity tests:** Verify a failure after transfer creation or
    after the first ledger entry rolls back the entire operation.
-   [ ] **Ledger invariants:** Test that every completed transfer has
    balanced debit and credit totals and positive entry amounts.
-   [ ] **Top-up trust boundary:** Keep simulated top-ups
    development-only. In production, require verified provider payment
    confirmation and deduplicate provider events.
-   [ ] **Schema verification:** Check all Flyway constraints,
    especially enum/status check constraints and system-account
    uniqueness, against the actual database.
-   [ ] **Authorization:** Test that users cannot fund, inspect, or
    transfer from wallets they do not own, and that admin endpoints
    require an admin role.
-   [ ] **Sensitive logging:** Ensure raw passwords, password hashes,
    authentication headers, and secrets are never logged.

### 1 --- Complete core wallet operations

-   [ ] Add a reliable system-account seeding migration and verify
    exactly one active account per system type.
-   [ ] Complete and test the simulated top-up DTO, service, controller,
    and response mapping.
-   [ ] Implement wallet balance and transaction-history read endpoints.
-   [ ] Implement withdrawals with pending states and confirmed payout
    semantics.
-   [ ] Implement fees and document how fees are represented in balanced
    ledger entries.
-   [ ] Implement reversals as new compensating ledger entries; do not
    edit or delete historical ledger entries.
-   [ ] Implement transfer-status transition rules and populate
    `transfer_status_audits`.
-   [ ] Add a consistent error response format and input validation
    tests.

### 2 --- Production-quality engineering

-   [ ] Integrate a payment provider for funding and payouts, including
    signature verification, webhook retries, and reconciliation.
-   [ ] Add fraud/risk decision handling for `CLEAR`, `FLAGGED`, and
    `HELD` outcomes.
-   [ ] Add reconciliation jobs and alerts for ledger, provider, and
    cached/projection discrepancies.
-   [ ] Add observability: structured logs without secrets, metrics,
    tracing, and health checks.
-   [ ] Add integration tests using a real PostgreSQL test instance (for
    example, Testcontainers).
-   [ ] Add API documentation and reproducible local setup instructions.
-   [ ] Review rate limiting, credential policy, TLS, secrets
    management, backups, and operational recovery.
-   [ ] Consider a cached balance projection only when justified by
    performance measurements. The ledger must remain authoritative.

## Production Readiness Checklist

This project should **not** be considered production-ready until the
critical correctness, security, provider integration, idempotency,
concurrency, reconciliation, and operational controls above have been
implemented and tested.

Before any production release, verify at minimum:

-   [ ] No publicly accessible simulated-money endpoint
-   [ ] Provider-confirmed funding and payout flows
-   [ ] Tokenized transaction creation and webhook processing
-   [ ] Concurrency-safe balance checks
-   [ ] Atomic balanced ledger posting
-   [ ] Reversals and audit history
-   [ ] Authorization and security tests
-   [ ] Database migration validation and backups
-   [ ] Monitoring, reconciliation, and incident recovery procedures
