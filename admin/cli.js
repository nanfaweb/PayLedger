#!/usr/bin/env node
/**
 * PayLedger developer admin CLI.
 * Requires DATABASE_URL (uses backend/.env by default).
 *
 * Commands:
 *   generate-codes --count 1000 --out codes.csv
 *   list-codes --status unused
 *   revoke-code --id <uuid>
 *   inspect-user --phone +923001234567
 *   issue-password-reset --phone +923001234567
 */
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { Client } = require('pg');

require('dotenv').config({ path: path.resolve(__dirname, '../backend/.env') });
require('dotenv').config({ path: path.resolve(__dirname, '../.env') });

function sha256(value) {
  return crypto.createHash('sha256').update(value, 'utf8').digest('hex');
}

function generateActivationCode() {
  const part = () => crypto.randomBytes(3).toString('hex').toUpperCase().slice(0, 4);
  return `PL-${part()}-${part()}-${part()}-${part()}`;
}

function generateResetToken() {
  return `PL-RESET-${crypto.randomBytes(18).toString('base64url')}`;
}

function normalizePhone(input) {
  const raw = String(input).trim().replace(/[\s\-()]/g, '');
  let digits = raw.startsWith('+') ? `+${raw.slice(1).replace(/\D/g, '')}` : raw.replace(/\D/g, '');
  let national;
  if (digits.startsWith('+92')) national = digits.slice(3);
  else if (digits.startsWith('92') && digits.length >= 12) national = digits.slice(2);
  else if (digits.startsWith('0')) national = digits.slice(1);
  else national = digits;
  if (!/^[3][0-9]{9}$/.test(national)) return null;
  return `+92${national}`;
}

function parseArgs(argv) {
  const args = { _: [] };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a.startsWith('--')) {
      const key = a.slice(2);
      const next = argv[i + 1];
      if (!next || next.startsWith('--')) {
        args[key] = true;
      } else {
        args[key] = next;
        i++;
      }
    } else {
      args._.push(a);
    }
  }
  return args;
}

async function withDb(fn) {
  const url = process.env.DATABASE_URL;
  if (!url) {
    console.error('DATABASE_URL required. Configure backend/.env');
    process.exit(1);
  }
  const client = new Client({ connectionString: url });
  await client.connect();
  try {
    return await fn(client);
  } finally {
    await client.end();
  }
}

async function generateCodes(args) {
  const count = Number(args.count || 1000);
  const out = path.resolve(args.out || path.join(__dirname, 'exports', `activation-codes-${Date.now()}.csv`));
  if (!Number.isInteger(count) || count < 1) {
    console.error('--count must be a positive integer');
    process.exit(1);
  }

  fs.mkdirSync(path.dirname(out), { recursive: true });

  const codes = [];
  const hashes = new Set();
  while (codes.length < count) {
    const code = generateActivationCode();
    const hash = sha256(code);
    if (hashes.has(hash)) continue;
    hashes.add(hash);
    codes.push({ code, hash });
  }

  await withDb(async (client) => {
    await client.query('BEGIN');
    try {
      for (const item of codes) {
        await client.query(
          `INSERT INTO activation_codes (code_hash, status) VALUES ($1, 'unused')`,
          [item.hash]
        );
      }
      await client.query('COMMIT');
    } catch (err) {
      await client.query('ROLLBACK');
      throw err;
    }
  });

  const csv = ['code,status\n', ...codes.map((c) => `${c.code},unused\n`)].join('');
  fs.writeFileSync(out, csv, { encoding: 'utf8', mode: 0o600 });
  console.log(`Generated ${codes.length} codes.`);
  console.log(`Plaintext CSV (developer only): ${out}`);
  console.log('Only hashes were stored in the database.');
}

async function listCodes(args) {
  const status = args.status || null;
  const limit = Number(args.limit || 50);
  await withDb(async (client) => {
    const params = [];
    let where = '';
    if (status) {
      params.push(status);
      where = `WHERE status = $1`;
    }
    params.push(limit);
    const { rows } = await client.query(
      `SELECT id, status, created_at, redeemed_at, redeemed_by_user_id, revoked_at
       FROM activation_codes ${where}
       ORDER BY created_at DESC
       LIMIT $${params.length}`,
      params
    );
    console.table(rows);
  });
}

async function revokeCode(args) {
  if (!args.id) {
    console.error('--id <uuid> required');
    process.exit(1);
  }
  await withDb(async (client) => {
    const { rows } = await client.query(
      `UPDATE activation_codes SET status = 'revoked', revoked_at = NOW()
       WHERE id = $1 AND status = 'unused'
       RETURNING id, status, revoked_at`,
      [args.id]
    );
    if (!rows.length) {
      console.error('No unused code found with that id');
      process.exit(1);
    }
    console.log('Revoked:', rows[0]);
  });
}

async function inspectUser(args) {
  const phone = normalizePhone(args.phone || '');
  if (!phone) {
    console.error('--phone required (Pakistani mobile)');
    process.exit(1);
  }
  await withDb(async (client) => {
    const { rows: users } = await client.query(
      `SELECT id, full_name, phone_number, preferred_language, created_at FROM users WHERE phone_number = $1`,
      [phone]
    );
    if (!users.length) {
      console.error('User not found');
      process.exit(1);
    }
    const user = users[0];
    const { rows: subs } = await client.query(`SELECT * FROM subscriptions WHERE user_id = $1`, [user.id]);
    const { rows: txnCount } = await client.query(
      `SELECT COUNT(*)::int AS count FROM transactions WHERE user_id = $1`,
      [user.id]
    );
    console.log('User:', user);
    console.log('Subscription:', subs[0] || null);
    console.log('Transactions:', txnCount[0].count);
  });
}

async function issuePasswordReset(args) {
  const phone = normalizePhone(args.phone || '');
  if (!phone) {
    console.error('--phone required (Pakistani mobile)');
    process.exit(1);
  }

  const token = generateResetToken();
  const tokenHash = sha256(token);
  const expiresAt = new Date(Date.now() + 30 * 60 * 1000);

  await withDb(async (client) => {
    const { rows } = await client.query(`SELECT id FROM users WHERE phone_number = $1`, [phone]);
    if (!rows.length) {
      console.error('User not found');
      process.exit(1);
    }
    const userId = rows[0].id;
    await client.query('BEGIN');
    try {
      await client.query(
        `UPDATE password_reset_tokens SET used_at = NOW() WHERE user_id = $1 AND used_at IS NULL`,
        [userId]
      );
      await client.query(
        `INSERT INTO password_reset_tokens (user_id, token_hash, expires_at) VALUES ($1, $2, $3)`,
        [userId, tokenHash, expiresAt.toISOString()]
      );
      await client.query('COMMIT');
    } catch (err) {
      await client.query('ROLLBACK');
      throw err;
    }
  });

  console.log('Password reset token (show once to user):');
  console.log(token);
  console.log(`Expires at: ${expiresAt.toISOString()} (30 minutes)`);
  console.log('User enters this in the app Forgot Password screen with their phone number.');
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const cmd = args._[0];
  switch (cmd) {
    case 'generate-codes':
      await generateCodes(args);
      break;
    case 'list-codes':
      await listCodes(args);
      break;
    case 'revoke-code':
      await revokeCode(args);
      break;
    case 'inspect-user':
      await inspectUser(args);
      break;
    case 'issue-password-reset':
      await issuePasswordReset(args);
      break;
    default:
      console.log(`PayLedger Admin CLI

Usage:
  node admin/cli.js generate-codes --count 1000 --out exports/codes.csv
  node admin/cli.js list-codes --status unused
  node admin/cli.js revoke-code --id <uuid>
  node admin/cli.js inspect-user --phone +923001234567
  node admin/cli.js issue-password-reset --phone +923001234567
`);
      process.exit(cmd ? 1 : 0);
  }
}

main().catch((err) => {
  console.error(err.message || err);
  process.exit(1);
});
