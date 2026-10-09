-- PayLedger: transactions and notification events

CREATE TYPE payment_provider AS ENUM ('jazzcash', 'easypaisa', 'sadapay', 'nayapay', 'other');
CREATE TYPE transaction_direction AS ENUM ('incoming', 'outgoing');
CREATE TYPE source_type AS ENUM ('notification', 'manual');
CREATE TYPE review_status AS ENUM ('confirmed', 'needs_review', 'manual');
CREATE TYPE notification_processing_status AS ENUM ('processed', 'ignored', 'duplicate', 'failed', 'needs_review');

CREATE TABLE IF NOT EXISTS transactions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider                payment_provider NOT NULL,
    sender_name             VARCHAR(200),
    sender_identifier       VARCHAR(64),
    amount                  NUMERIC(14, 2) NOT NULL CHECK (amount >= 0),
    currency                VARCHAR(8) NOT NULL DEFAULT 'PKR',
    transaction_reference   VARCHAR(128),
    transaction_timestamp   TIMESTAMPTZ NOT NULL,
    received_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    transaction_direction   transaction_direction NOT NULL DEFAULT 'incoming',
    original_notification   TEXT,
    source_type             source_type NOT NULL DEFAULT 'notification',
    notes                   TEXT,
    review_status           review_status NOT NULL DEFAULT 'confirmed',
    deduplication_key       TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_transactions_user_timestamp
    ON transactions (user_id, transaction_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_user_provider
    ON transactions (user_id, provider);
CREATE INDEX IF NOT EXISTS idx_transactions_user_amount
    ON transactions (user_id, amount);
CREATE INDEX IF NOT EXISTS idx_transactions_user_sender_name
    ON transactions (user_id, sender_name);
CREATE INDEX IF NOT EXISTS idx_transactions_user_sender_identifier
    ON transactions (user_id, sender_identifier);
CREATE INDEX IF NOT EXISTS idx_transactions_user_reference
    ON transactions (user_id, transaction_reference);
CREATE UNIQUE INDEX IF NOT EXISTS idx_transactions_user_dedupe
    ON transactions (user_id, deduplication_key)
    WHERE deduplication_key IS NOT NULL;

DROP TRIGGER IF EXISTS trg_transactions_updated_at ON transactions;
CREATE TRIGGER trg_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW
    EXECUTE PROCEDURE set_updated_at();

CREATE TABLE IF NOT EXISTS notification_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider            payment_provider NOT NULL,
    deduplication_key   TEXT NOT NULL,
    processing_status   notification_processing_status NOT NULL,
    received_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    title               TEXT,
    text_body           TEXT,
    CONSTRAINT notification_events_user_dedupe_unique UNIQUE (user_id, deduplication_key)
);

CREATE INDEX IF NOT EXISTS idx_notification_events_user_received
    ON notification_events (user_id, received_at DESC);

-- Per-user provider monitoring preferences
CREATE TABLE IF NOT EXISTS user_provider_settings (
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider            payment_provider NOT NULL,
    monitoring_enabled  BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, provider)
);
