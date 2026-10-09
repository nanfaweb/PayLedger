const { query, withTransaction } = require('../db');
const { createHttpError } = require('../middleware/error');

const PROVIDERS = ['jazzcash', 'easypaisa', 'sadapay', 'nayapay', 'other'];

function mapTransaction(row) {
  return {
    id: row.id,
    provider: row.provider,
    sender_name: row.sender_name,
    sender_identifier: row.sender_identifier,
    amount: Number(row.amount),
    currency: row.currency,
    transaction_reference: row.transaction_reference,
    transaction_timestamp: row.transaction_timestamp,
    received_at: row.received_at,
    transaction_direction: row.transaction_direction,
    original_notification: row.original_notification,
    source_type: row.source_type,
    notes: row.notes,
    review_status: row.review_status,
    deduplication_key: row.deduplication_key,
    created_at: row.created_at,
    updated_at: row.updated_at,
  };
}

async function createTransaction(userId, input) {
  if (!PROVIDERS.includes(input.provider)) {
    throw createHttpError(400, 'Invalid provider', 'INVALID_PROVIDER');
  }
  if (input.amount == null || Number(input.amount) < 0) {
    throw createHttpError(400, 'Invalid amount', 'INVALID_AMOUNT');
  }

  const dedupe = input.deduplication_key || null;

  try {
    const { rows } = await query(
      `INSERT INTO transactions (
         user_id, provider, sender_name, sender_identifier, amount, currency,
         transaction_reference, transaction_timestamp, received_at,
         transaction_direction, original_notification, source_type, notes,
         review_status, deduplication_key
       ) VALUES (
         $1,$2,$3,$4,$5,$6,$7,$8,COALESCE($9, NOW()),$10,$11,$12,$13,$14,$15
       )
       RETURNING *`,
      [
        userId,
        input.provider,
        input.sender_name || null,
        input.sender_identifier || null,
        input.amount,
        input.currency || 'PKR',
        input.transaction_reference || null,
        input.transaction_timestamp || new Date().toISOString(),
        input.received_at || null,
        input.transaction_direction || 'incoming',
        input.original_notification || null,
        input.source_type || 'manual',
        input.notes || null,
        input.review_status || (input.source_type === 'manual' ? 'manual' : 'confirmed'),
        dedupe,
      ]
    );
    return mapTransaction(rows[0]);
  } catch (err) {
    if (err.code === '23505') {
      throw createHttpError(409, 'Duplicate transaction', 'DUPLICATE');
    }
    throw err;
  }
}

async function recordNotificationEvent(userId, { provider, deduplication_key, processing_status, title, text_body }) {
  try {
    await query(
      `INSERT INTO notification_events
         (user_id, provider, deduplication_key, processing_status, title, text_body)
       VALUES ($1,$2,$3,$4,$5,$6)
       ON CONFLICT (user_id, deduplication_key) DO NOTHING`,
      [userId, provider, deduplication_key, processing_status, title || null, text_body || null]
    );
  } catch {
    // non-fatal audit log
  }
}

async function getTransaction(userId, id) {
  const { rows } = await query(`SELECT * FROM transactions WHERE id = $1 AND user_id = $2`, [id, userId]);
  if (!rows.length) {
    throw createHttpError(404, 'Transaction not found', 'NOT_FOUND');
  }
  return mapTransaction(rows[0]);
}

async function updateTransaction(userId, id, patch) {
  const allowed = [
    'sender_name',
    'sender_identifier',
    'amount',
    'notes',
    'review_status',
    'transaction_reference',
    'transaction_direction',
    'provider',
  ];
  const fields = [];
  const values = [];
  let i = 1;

  for (const key of allowed) {
    if (Object.prototype.hasOwnProperty.call(patch, key) && patch[key] !== undefined) {
      fields.push(`${key} = $${i++}`);
      values.push(patch[key]);
    }
  }

  if (!fields.length) {
    throw createHttpError(400, 'No fields to update', 'NO_UPDATE');
  }

  values.push(id);
  values.push(userId);

  const { rows } = await query(
    `UPDATE transactions SET ${fields.join(', ')}, updated_at = NOW()
     WHERE id = $${i++} AND user_id = $${i}
     RETURNING *`,
    values
  );

  if (!rows.length) {
    throw createHttpError(404, 'Transaction not found', 'NOT_FOUND');
  }
  return mapTransaction(rows[0]);
}

async function deleteTransaction(userId, id) {
  const { rowCount } = await query(`DELETE FROM transactions WHERE id = $1 AND user_id = $2`, [id, userId]);
  if (!rowCount) {
    throw createHttpError(404, 'Transaction not found', 'NOT_FOUND');
  }
  return { ok: true };
}

async function searchTransactions(userId, filters = {}) {
  const {
    q,
    sender_name,
    sender_identifier,
    amount,
    amount_min,
    amount_max,
    reference,
    provider,
    direction,
    review_status,
    date_from,
    date_to,
    sort = 'newest',
    page = 1,
    page_size = 20,
  } = filters;

  const where = ['user_id = $1'];
  const params = [userId];
  let i = 2;

  if (sender_name) {
    where.push(`sender_name ILIKE $${i++}`);
    params.push(`%${sender_name}%`);
  }
  if (sender_identifier) {
    where.push(`sender_identifier ILIKE $${i++}`);
    params.push(`%${sender_identifier}%`);
  }
  if (q) {
    where.push(`(sender_name ILIKE $${i} OR sender_identifier ILIKE $${i} OR transaction_reference ILIKE $${i})`);
    params.push(`%${q}%`);
    i++;
  }
  if (amount != null && amount !== '') {
    where.push(`amount = $${i++}`);
    params.push(amount);
  }
  if (amount_min != null && amount_min !== '') {
    where.push(`amount >= $${i++}`);
    params.push(amount_min);
  }
  if (amount_max != null && amount_max !== '') {
    where.push(`amount <= $${i++}`);
    params.push(amount_max);
  }
  if (reference) {
    where.push(`transaction_reference ILIKE $${i++}`);
    params.push(`%${reference}%`);
  }
  if (provider) {
    where.push(`provider = $${i++}`);
    params.push(provider);
  }
  if (direction) {
    where.push(`transaction_direction = $${i++}`);
    params.push(direction);
  }
  if (review_status) {
    where.push(`review_status = $${i++}`);
    params.push(review_status);
  }
  if (date_from) {
    where.push(`transaction_timestamp >= $${i++}`);
    params.push(date_from);
  }
  if (date_to) {
    where.push(`transaction_timestamp <= $${i++}`);
    params.push(date_to);
  }

  const order = sort === 'oldest' ? 'ASC' : 'DESC';
  const limit = Math.min(Math.max(Number(page_size) || 20, 1), 100);
  const offset = (Math.max(Number(page) || 1, 1) - 1) * limit;

  const whereSql = where.join(' AND ');

  const countResult = await query(`SELECT COUNT(*)::int AS total FROM transactions WHERE ${whereSql}`, params);
  const total = countResult.rows[0].total;

  params.push(limit);
  params.push(offset);
  const { rows } = await query(
    `SELECT * FROM transactions
     WHERE ${whereSql}
     ORDER BY transaction_timestamp ${order}
     LIMIT $${i++} OFFSET $${i}`,
    params
  );

  return {
    items: rows.map(mapTransaction),
    page: Math.max(Number(page) || 1, 1),
    page_size: limit,
    total,
    total_pages: Math.ceil(total / limit) || 0,
  };
}

async function getSummary(userId, { timezone = 'Asia/Karachi' } = {}) {
  // Use timestamptz comparisons relative to Pakistan business day
  const { rows: totals } = await query(
    `SELECT
       COALESCE(SUM(amount) FILTER (
         WHERE transaction_direction = 'incoming'
           AND transaction_timestamp >= date_trunc('day', NOW() AT TIME ZONE $2) AT TIME ZONE $2
       ), 0) AS today_total,
       COALESCE(SUM(amount) FILTER (
         WHERE transaction_direction = 'incoming'
           AND transaction_timestamp >= date_trunc('week', NOW() AT TIME ZONE $2) AT TIME ZONE $2
       ), 0) AS week_total,
       COALESCE(SUM(amount) FILTER (
         WHERE transaction_direction = 'incoming'
           AND transaction_timestamp >= date_trunc('month', NOW() AT TIME ZONE $2) AT TIME ZONE $2
       ), 0) AS month_total,
       COUNT(*) FILTER (WHERE transaction_direction = 'incoming')::int AS transaction_count
     FROM transactions
     WHERE user_id = $1`,
    [userId, timezone]
  );

  const { rows: byProvider } = await query(
    `SELECT provider, COALESCE(SUM(amount), 0) AS total, COUNT(*)::int AS count
     FROM transactions
     WHERE user_id = $1 AND transaction_direction = 'incoming'
     GROUP BY provider
     ORDER BY provider`,
    [userId]
  );

  const { rows: recent } = await query(
    `SELECT * FROM transactions
     WHERE user_id = $1
     ORDER BY transaction_timestamp DESC
     LIMIT 10`,
    [userId]
  );

  return {
    today_total: Number(totals[0].today_total),
    week_total: Number(totals[0].week_total),
    month_total: Number(totals[0].month_total),
    transaction_count: totals[0].transaction_count,
    currency: 'PKR',
    by_provider: byProvider.map((r) => ({
      provider: r.provider,
      total: Number(r.total),
      count: r.count,
    })),
    recent: recent.map(mapTransaction),
  };
}

async function getProviderSettings(userId) {
  const defaults = ['jazzcash', 'easypaisa', 'sadapay', 'nayapay'];
  const { rows } = await query(
    `SELECT provider, monitoring_enabled FROM user_provider_settings WHERE user_id = $1`,
    [userId]
  );
  const map = Object.fromEntries(rows.map((r) => [r.provider, r.monitoring_enabled]));
  return defaults.map((p) => ({
    provider: p,
    monitoring_enabled: map[p] !== undefined ? map[p] : true,
  }));
}

async function setProviderSetting(userId, provider, enabled) {
  if (!['jazzcash', 'easypaisa', 'sadapay', 'nayapay'].includes(provider)) {
    throw createHttpError(400, 'Invalid provider', 'INVALID_PROVIDER');
  }
  await query(
    `INSERT INTO user_provider_settings (user_id, provider, monitoring_enabled, updated_at)
     VALUES ($1, $2, $3, NOW())
     ON CONFLICT (user_id, provider)
     DO UPDATE SET monitoring_enabled = EXCLUDED.monitoring_enabled, updated_at = NOW()`,
    [userId, provider, !!enabled]
  );
  return getProviderSettings(userId);
}

async function getLastNotificationStatus(userId) {
  const { rows } = await query(
    `SELECT provider, processing_status, received_at
     FROM notification_events
     WHERE user_id = $1
     ORDER BY received_at DESC
     LIMIT 1`,
    [userId]
  );
  return rows[0] || null;
}

module.exports = {
  createTransaction,
  recordNotificationEvent,
  getTransaction,
  updateTransaction,
  deleteTransaction,
  searchTransactions,
  getSummary,
  getProviderSettings,
  setProviderSetting,
  getLastNotificationStatus,
  mapTransaction,
  withTransaction,
};
