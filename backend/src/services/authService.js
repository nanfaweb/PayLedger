const { query, withTransaction } = require('../db');
const { hashPassword, verifyPassword, sha256, generateResetToken } = require('../utils/crypto');
const { normalizePakistaniPhone } = require('../utils/phone');
const {
  signAccessToken,
  createRefreshTokenValue,
  hashRefreshToken,
  refreshExpiryDate,
} = require('../utils/tokens');
const { createHttpError } = require('../middleware/error');

async function issueSession(user, userAgent) {
  const refreshToken = createRefreshTokenValue();
  const tokenHash = hashRefreshToken(refreshToken);
  const expiresAt = refreshExpiryDate();

  await query(
    `INSERT INTO refresh_sessions (user_id, token_hash, expires_at, user_agent)
     VALUES ($1, $2, $3, $4)`,
    [user.id, tokenHash, expiresAt.toISOString(), userAgent || null]
  );

  return {
    access_token: signAccessToken(user),
    refresh_token: refreshToken,
    token_type: 'Bearer',
    expires_in: Number(process.env.ACCESS_TOKEN_TTL_SECONDS || 900),
    user: publicUser(user),
  };
}

function publicUser(user) {
  return {
    id: user.id,
    full_name: user.full_name,
    phone_number: user.phone_number,
    preferred_language: user.preferred_language,
  };
}

async function register({ fullName, phoneNumber, password }) {
  const phone = normalizePakistaniPhone(phoneNumber);
  if (!phone) {
    throw createHttpError(400, 'Invalid Pakistani phone number', 'INVALID_PHONE');
  }
  if (!password || password.length < 8) {
    throw createHttpError(400, 'Password must be at least 8 characters', 'WEAK_PASSWORD');
  }
  if (!fullName || fullName.trim().length < 2) {
    throw createHttpError(400, 'Full name is required', 'INVALID_NAME');
  }

  const passwordHash = await hashPassword(password);

  try {
    const { rows } = await query(
      `INSERT INTO users (full_name, phone_number, password_hash)
       VALUES ($1, $2, $3)
       RETURNING id, full_name, phone_number, preferred_language`,
      [fullName.trim(), phone, passwordHash]
    );
    return rows[0];
  } catch (err) {
    if (err.code === '23505') {
      throw createHttpError(409, 'Phone number already registered', 'PHONE_TAKEN');
    }
    throw err;
  }
}

async function login({ phoneNumber, password, userAgent }) {
  const phone = normalizePakistaniPhone(phoneNumber);
  if (!phone) {
    throw createHttpError(400, 'Invalid Pakistani phone number', 'INVALID_PHONE');
  }

  const { rows } = await query(
    `SELECT id, full_name, phone_number, preferred_language, password_hash
     FROM users WHERE phone_number = $1`,
    [phone]
  );

  // Constant-ish failure path
  if (!rows.length) {
    throw createHttpError(401, 'Invalid phone number or password', 'INVALID_CREDENTIALS');
  }

  const user = rows[0];
  const ok = await verifyPassword(user.password_hash, password);
  if (!ok) {
    throw createHttpError(401, 'Invalid phone number or password', 'INVALID_CREDENTIALS');
  }

  return issueSession(user, userAgent);
}

async function registerAndLogin(input, userAgent) {
  const user = await register(input);
  return issueSession(user, userAgent);
}

async function refresh(refreshToken, userAgent) {
  if (!refreshToken) {
    throw createHttpError(400, 'Refresh token required', 'MISSING_TOKEN');
  }
  const tokenHash = hashRefreshToken(refreshToken);

  return withTransaction(async (client) => {
    const { rows } = await client.query(
      `SELECT rs.id, rs.user_id, rs.expires_at, rs.revoked_at,
              u.full_name, u.phone_number, u.preferred_language
       FROM refresh_sessions rs
       JOIN users u ON u.id = rs.user_id
       WHERE rs.token_hash = $1
       FOR UPDATE OF rs`,
      [tokenHash]
    );

    if (!rows.length) {
      throw createHttpError(401, 'Invalid refresh token', 'INVALID_REFRESH');
    }

    const session = rows[0];
    if (session.revoked_at || new Date(session.expires_at) <= new Date()) {
      throw createHttpError(401, 'Refresh token expired or revoked', 'INVALID_REFRESH');
    }

    // Rotate refresh token
    await client.query(`UPDATE refresh_sessions SET revoked_at = NOW() WHERE id = $1`, [session.id]);

    const newRefresh = createRefreshTokenValue();
    const newHash = hashRefreshToken(newRefresh);
    const expiresAt = refreshExpiryDate();
    await client.query(
      `INSERT INTO refresh_sessions (user_id, token_hash, expires_at, user_agent)
       VALUES ($1, $2, $3, $4)`,
      [session.user_id, newHash, expiresAt.toISOString(), userAgent || null]
    );

    const user = {
      id: session.user_id,
      full_name: session.full_name,
      phone_number: session.phone_number,
      preferred_language: session.preferred_language,
    };

    return {
      access_token: signAccessToken(user),
      refresh_token: newRefresh,
      token_type: 'Bearer',
      expires_in: Number(process.env.ACCESS_TOKEN_TTL_SECONDS || 900),
      user: publicUser(user),
    };
  });
}

async function logout(refreshToken) {
  if (!refreshToken) return;
  const tokenHash = hashRefreshToken(refreshToken);
  await query(
    `UPDATE refresh_sessions SET revoked_at = NOW()
     WHERE token_hash = $1 AND revoked_at IS NULL`,
    [tokenHash]
  );
}

async function logoutAll(userId) {
  await query(
    `UPDATE refresh_sessions SET revoked_at = NOW()
     WHERE user_id = $1 AND revoked_at IS NULL`,
    [userId]
  );
}

/**
 * Admin CLI uses createPasswordResetToken; app uses resetPasswordWithToken.
 */
async function createPasswordResetToken(phoneNumber) {
  const phone = normalizePakistaniPhone(phoneNumber);
  if (!phone) {
    throw createHttpError(400, 'Invalid Pakistani phone number', 'INVALID_PHONE');
  }

  const { rows } = await query(`SELECT id FROM users WHERE phone_number = $1`, [phone]);
  if (!rows.length) {
    throw createHttpError(404, 'User not found', 'USER_NOT_FOUND');
  }

  const userId = rows[0].id;
  const token = generateResetToken();
  const tokenHash = sha256(token);
  const expiresAt = new Date(Date.now() + 30 * 60 * 1000);

  await withTransaction(async (client) => {
    // Invalidate previous unused tokens
    await client.query(
      `UPDATE password_reset_tokens SET used_at = NOW()
       WHERE user_id = $1 AND used_at IS NULL`,
      [userId]
    );
    await client.query(
      `INSERT INTO password_reset_tokens (user_id, token_hash, expires_at)
       VALUES ($1, $2, $3)`,
      [userId, tokenHash, expiresAt.toISOString()]
    );
  });

  return { token, expires_at: expiresAt.toISOString(), phone_number: phone };
}

async function resetPasswordWithToken({ phoneNumber, resetToken, newPassword }) {
  const phone = normalizePakistaniPhone(phoneNumber);
  if (!phone) {
    throw createHttpError(400, 'Invalid Pakistani phone number', 'INVALID_PHONE');
  }
  if (!newPassword || newPassword.length < 8) {
    throw createHttpError(400, 'Password must be at least 8 characters', 'WEAK_PASSWORD');
  }
  if (!resetToken) {
    throw createHttpError(400, 'Reset token required', 'MISSING_TOKEN');
  }

  const tokenHash = sha256(resetToken);

  await withTransaction(async (client) => {
    const { rows } = await client.query(
      `SELECT prt.id AS token_id, u.id AS user_id, prt.expires_at, prt.used_at
       FROM password_reset_tokens prt
       JOIN users u ON u.id = prt.user_id
       WHERE prt.token_hash = $1 AND u.phone_number = $2
       FOR UPDATE OF prt`,
      [tokenHash, phone]
    );

    if (!rows.length) {
      throw createHttpError(400, 'Invalid reset token', 'INVALID_RESET_TOKEN');
    }

    const row = rows[0];
    if (row.used_at) {
      throw createHttpError(400, 'Reset token already used', 'INVALID_RESET_TOKEN');
    }
    if (new Date(row.expires_at) <= new Date()) {
      throw createHttpError(400, 'Reset token expired', 'INVALID_RESET_TOKEN');
    }

    const passwordHash = await hashPassword(newPassword);
    await client.query(`UPDATE users SET password_hash = $1, updated_at = NOW() WHERE id = $2`, [
      passwordHash,
      row.user_id,
    ]);
    await client.query(`UPDATE password_reset_tokens SET used_at = NOW() WHERE id = $1`, [row.token_id]);
    await client.query(
      `UPDATE refresh_sessions SET revoked_at = NOW()
       WHERE user_id = $1 AND revoked_at IS NULL`,
      [row.user_id]
    );
  });

  return { ok: true };
}

async function deleteAccount(userId) {
  await query(`DELETE FROM users WHERE id = $1`, [userId]);
}

async function updateProfile(userId, { fullName, preferredLanguage }) {
  const fields = [];
  const values = [];
  let i = 1;

  if (fullName != null) {
    fields.push(`full_name = $${i++}`);
    values.push(fullName.trim());
  }
  if (preferredLanguage != null) {
    if (!['en', 'ur'].includes(preferredLanguage)) {
      throw createHttpError(400, 'Language must be en or ur', 'INVALID_LANGUAGE');
    }
    fields.push(`preferred_language = $${i++}`);
    values.push(preferredLanguage);
  }

  if (!fields.length) {
    throw createHttpError(400, 'No fields to update', 'NO_UPDATE');
  }

  values.push(userId);
  const { rows } = await query(
    `UPDATE users SET ${fields.join(', ')}, updated_at = NOW()
     WHERE id = $${i}
     RETURNING id, full_name, phone_number, preferred_language`,
    values
  );
  return rows[0];
}

module.exports = {
  register,
  registerAndLogin,
  login,
  refresh,
  logout,
  logoutAll,
  createPasswordResetToken,
  resetPasswordWithToken,
  deleteAccount,
  updateProfile,
  publicUser,
  issueSession,
};
