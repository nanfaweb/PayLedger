const { verifyAccessToken } = require('../utils/tokens');
const { query } = require('../db');

async function requireAuth(req, res, next) {
  try {
    const header = req.headers.authorization || '';
    const [scheme, token] = header.split(' ');
    if (scheme !== 'Bearer' || !token) {
      return res.status(401).json({ error: 'Authentication required' });
    }

    let payload;
    try {
      payload = verifyAccessToken(token);
    } catch {
      return res.status(401).json({ error: 'Invalid or expired access token' });
    }

    if (payload.typ !== 'access' || !payload.sub) {
      return res.status(401).json({ error: 'Invalid access token' });
    }

    const { rows } = await query('SELECT id, full_name, phone_number, preferred_language FROM users WHERE id = $1', [
      payload.sub,
    ]);
    if (!rows.length) {
      return res.status(401).json({ error: 'User not found' });
    }

    req.user = rows[0];
    req.userId = rows[0].id;
    return next();
  } catch (err) {
    return next(err);
  }
}

module.exports = { requireAuth };
