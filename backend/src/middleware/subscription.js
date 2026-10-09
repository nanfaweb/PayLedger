const { query } = require('../db');

/**
 * Server-side subscription gate. Never trust the client for license validity.
 */
async function requireActiveSubscription(req, res, next) {
  try {
    const { rows } = await query(
      `SELECT id, activated_at, expires_at, status, last_renewed_at
       FROM subscriptions
       WHERE user_id = $1`,
      [req.userId]
    );

    if (!rows.length) {
      return res.status(403).json({
        error: 'Subscription required',
        code: 'SUBSCRIPTION_REQUIRED',
      });
    }

    const sub = rows[0];
    const now = new Date();
    const expiresAt = new Date(sub.expires_at);

    if (expiresAt <= now || sub.status !== 'active') {
      if (sub.status === 'active') {
        await query(`UPDATE subscriptions SET status = 'expired', updated_at = NOW() WHERE id = $1`, [sub.id]);
      }
      return res.status(403).json({
        error: 'Subscription expired',
        code: 'SUBSCRIPTION_EXPIRED',
        expires_at: sub.expires_at,
      });
    }

    req.subscription = sub;
    return next();
  } catch (err) {
    return next(err);
  }
}

module.exports = { requireActiveSubscription };
