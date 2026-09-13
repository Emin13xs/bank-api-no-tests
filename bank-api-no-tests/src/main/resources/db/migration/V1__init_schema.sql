CREATE TABLE accounts (
    id              UUID PRIMARY KEY,
    account_number  VARCHAR(34)     NOT NULL UNIQUE,
    owner_name      VARCHAR(255)    NOT NULL,
    balance         NUMERIC(19, 4)  NOT NULL CHECK (balance >= 0),
    currency        CHAR(3)         NOT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE TABLE transactions (
    id                UUID PRIMARY KEY,
    type              VARCHAR(16)     NOT NULL,
    status            VARCHAR(16)     NOT NULL,
    from_account_id   UUID            REFERENCES accounts (id),
    to_account_id     UUID            REFERENCES accounts (id),
    amount            NUMERIC(19, 4)  NOT NULL CHECK (amount > 0),
    currency          CHAR(3)         NOT NULL,
    idempotency_key   VARCHAR(128)    NOT NULL,
    failure_reason    TEXT,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    completed_at      TIMESTAMPTZ,

    CONSTRAINT uk_transactions_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_transactions_from_account ON transactions (from_account_id);
CREATE INDEX idx_transactions_to_account ON transactions (to_account_id);
CREATE INDEX idx_accounts_account_number ON accounts (account_number);
