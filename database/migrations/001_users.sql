-- PayLedger: users
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name       VARCHAR(120) NOT NULL,
    phone_number    VARCHAR(16) NOT NULL,
    password_hash   TEXT NOT NULL,
    preferred_language VARCHAR(8) NOT NULL DEFAULT 'en'
        CHECK (preferred_language IN ('en', 'ur')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT users_phone_number_unique UNIQUE (phone_number),
    CONSTRAINT users_phone_e164_format CHECK (phone_number ~ '^\+92[0-9]{10}$')
);

CREATE INDEX IF NOT EXISTS idx_users_phone_number ON users (phone_number);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE PROCEDURE set_updated_at();
