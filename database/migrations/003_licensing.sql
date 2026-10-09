-- PayLedger: activation codes and subscriptions

CREATE TYPE activation_code_status AS ENUM ('unused', 'redeemed', 'revoked', 'expired');

CREATE TABLE IF NOT EXISTS activation_codes (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_hash            TEXT NOT NULL,
    status               activation_code_status NOT NULL DEFAULT 'unused',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    redeemed_by_user_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    redeemed_at          TIMESTAMPTZ,
    expires_at           TIMESTAMPTZ,
    revoked_at           TIMESTAMPTZ,
    CONSTRAINT activation_codes_code_hash_unique UNIQUE (code_hash)
);

CREATE INDEX IF NOT EXISTS idx_activation_codes_status ON activation_codes (status);
CREATE INDEX IF NOT EXISTS idx_activation_codes_redeemed_by ON activation_codes (redeemed_by_user_id);

CREATE TYPE subscription_status AS ENUM ('active', 'expired', 'none');

CREATE TABLE IF NOT EXISTS subscriptions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activated_at     TIMESTAMPTZ NOT NULL,
    expires_at       TIMESTAMPTZ NOT NULL,
    status           subscription_status NOT NULL DEFAULT 'active',
    last_renewed_at  TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT subscriptions_user_id_unique UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_expires_at ON subscriptions (expires_at);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions (status);

DROP TRIGGER IF EXISTS trg_subscriptions_updated_at ON subscriptions;
CREATE TRIGGER trg_subscriptions_updated_at
    BEFORE UPDATE ON subscriptions
    FOR EACH ROW
    EXECUTE PROCEDURE set_updated_at();
