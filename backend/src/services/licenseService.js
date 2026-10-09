const { query, withTransaction } = require('../db');
const { sha256, generateActivationCode } = require('../utils/crypto');
const { createHttpError } = require('../middleware/error');

const SUBSCRIPTION_DAYS = 365;

function daysRemaining(expiresAt) {
  const ms = new Date(expiresAt).getTime() - Date.now();
  return Math.max(0, Math.ceil(ms / (24 * 60 * 60 * 1000)));
}

async function getLicenseStatus(userId) {
  const { rows } = await query(
    `SELECT id, activated_at, expires_at, status, last_renewed_at
     FROM subscriptions WHERE user_id = $1`,
    [userId]
  );

  if (!rows.length) {
    return {
      active: false,
      status: 'none',
      activated_at: null,
      expires_at: null,
      days_remaining: 0,
      last_renewed_at: null,
    };
  }

  const sub = rows[0];
  const now = new Date();
  const expired = new Date(sub.expires_at) <= now || sub.status !== 'active';

  if (expired && sub.status === 'active') {
    await query(`UPDATE subscriptions SET status = 'expired', updated_at = NOW() WHERE id = $1`, [sub.id]);
    sub.status = 'expired';
  }

  return {
    active: !expired,
    status: expired ? 'expired' : 'active',
    activated_at: sub.activated_at,
    expires_at: sub.expires_at,
    days_remaining: expired ? 0 : daysRemaining(sub.expires_at),
    last_renewed_at: sub.last_renewed_at,
  };
}

/**
 * Atomically redeem an activation code.
 * Extends from max(now, current_expires_at) + 365 days.
 */
async function activateCode(userId, plaintextCode) {
  if (!plaintextCode || typeof plaintextCode !== 'string') {
    throw createHttpError(400, 'Activation code required', 'MISSING_CODE');
  }

  const normalized = plaintextCode.trim().toUpperCase();
  const codeHash = sha256(normalized);

  return withTransaction(async (client) => {
    const { rows: codeRows } = await client.query(
      `SELECT id, status, expires_at, redeemed_by_user_id, revoked_at
       FROM activation_codes
       WHERE code_hash = $1
       FOR UPDATE`,
      [codeHash]
    );

    if (!codeRows.length) {
      throw createHttpError(400, 'Invalid activation code', 'INVALID_CODE');
    }

    const code = codeRows[0];

    if (code.status === 'revoked' || code.revoked_at) {
      throw createHttpError(400, 'This activation code has been revoked', 'CODE_REVOKED');
    }
    if (code.status === 'redeemed') {
      throw createHttpError(400, 'This activation code has already been used', 'CODE_REDEEMED');
    }
    if (code.status !== 'unused') {
      throw createHttpError(400, 'This activation code is not eligible', 'CODE_INELIGIBLE');
    }
    if (code.expires_at && new Date(code.expires_at) <= new Date()) {
      await client.query(
        `UPDATE activation_codes SET status = 'expired' WHERE id = $1`,
        [code.id]
      );
      throw createHttpError(400, 'This activation code has expired', 'CODE_EXPIRED');
    }

    const now = new Date();
    const { rows: subRows } = await client.query(
      `SELECT id, expires_at, status FROM subscriptions WHERE user_id = $1 FOR UPDATE`,
      [userId]
    );

    let activatedAt = now;
    let expiresAt;
    let lastRenewedAt = null;

    if (!subRows.length) {
      expiresAt = new Date(now.getTime() + SUBSCRIPTION_DAYS * 24 * 60 * 60 * 1000);
      await client.query(
        `INSERT INTO subscriptions (user_id, activated_at, expires_at, status)
         VALUES ($1, $2, $3, 'active')`,
        [userId, activatedAt.toISOString(), expiresAt.toISOString()]
      );
    } else {
      const sub = subRows[0];
      const currentExpiry = new Date(sub.expires_at);
      const base = currentExpiry > now ? currentExpiry : now;
      expiresAt = new Date(base.getTime() + SUBSCRIPTION_DAYS * 24 * 60 * 60 * 1000);
      lastRenewedAt = now;
      activatedAt = sub.activated_at ? new Date(sub.activated_at) : now;

      await client.query(
        `UPDATE subscriptions
         SET expires_at = $1, status = 'active', last_renewed_at = $2, updated_at = NOW()
         WHERE id = $3`,
        [expiresAt.toISOString(), lastRenewedAt.toISOString(), sub.id]
      );
    }

    await client.query(
      `UPDATE activation_codes
       SET status = 'redeemed',
           redeemed_by_user_id = $1,
           redeemed_at = $2
       WHERE id = $3`,
      [userId, now.toISOString(), code.id]
    );

    return {
      active: true,
      status: 'active',
      activated_at: activatedAt.toISOString(),
      expires_at: expiresAt.toISOString(),
      days_remaining: daysRemaining(expiresAt),
      last_renewed_at: lastRenewedAt ? lastRenewedAt.toISOString() : null,
    };
  });
}

async function generateCodes(count, { codeExpiresAt = null } = {}) {
  if (!Number.isInteger(count) || count < 1 || count > 10000) {
    throw createHttpError(400, 'Count must be between 1 and 10000', 'INVALID_COUNT');
  }

  const codes = [];
  const hashes = new Set();

  while (codes.length < count) {
    const code = generateActivationCode();
    const hash = sha256(code);
    if (hashes.has(hash)) continue;
    hashes.add(hash);
    codes.push({ code, hash });
  }

  // Insert in batches
  const client = await require('../db').pool.connect();
  try {
    await client.query('BEGIN');
    for (const item of codes) {
      await client.query(
        `INSERT INTO activation_codes (code_hash, status, expires_at)
         VALUES ($1, 'unused', $2)`,
        [item.hash, codeExpiresAt]
      );
    }
    await client.query('COMMIT');
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }

  return codes.map((c) => c.code);
}

async function revokeCodeById(id) {
  const { rows } = await query(
    `UPDATE activation_codes
     SET status = 'revoked', revoked_at = NOW()
     WHERE id = $1 AND status = 'unused'
     RETURNING id, status`,
    [id]
  );
  if (!rows.length) {
    throw createHttpError(404, 'Unused code not found', 'NOT_FOUND');
  }
  return rows[0];
}

async function listCodes({ status, limit = 100, offset = 0 } = {}) {
  const params = [];
  let where = '';
  if (status) {
    params.push(status);
    where = `WHERE status = $${params.length}`;
  }
  params.push(limit);
  params.push(offset);
  const { rows } = await query(
    `SELECT id, status, created_at, redeemed_by_user_id, redeemed_at, expires_at, revoked_at
     FROM activation_codes
     ${where}
     ORDER BY created_at DESC
     LIMIT $${params.length - 1} OFFSET $${params.length}`,
    params
  );
  return rows;
}

module.exports = {
  getLicenseStatus,
  activateCode,
  generateCodes,
  revokeCodeById,
  listCodes,
  SUBSCRIPTION_DAYS,
  daysRemaining,
};
