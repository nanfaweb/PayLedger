const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const request = require('supertest');

process.env.NODE_ENV = 'test';

let app;
let dbAvailable = false;
let { createApp } = {};

before(async () => {
  try {
    const { pool } = require('../../src/db');
    await pool.query('SELECT 1');
    dbAvailable = true;
    ({ createApp } = require('../../src/app'));
    app = createApp();
  } catch (err) {
    console.warn('Skipping isolation tests:', err.message);
  }
});

after(async () => {
  try {
    const { pool } = require('../../src/db');
    await pool.end();
  } catch {
    // ignore
  }
});

describe('user data isolation', () => {
  it('prevents user B from reading user A transactions', async (t) => {
    if (!dbAvailable) return t.skip('database unavailable');

    const suffix = String(Date.now()).slice(-7);
    const userA = await request(app).post('/auth/register').send({
      full_name: 'User A',
      phone_number: `0302${suffix}`,
      password: 'SecurePass1!',
      confirm_password: 'SecurePass1!',
    });
    const userB = await request(app).post('/auth/register').send({
      full_name: 'User B',
      phone_number: `0303${suffix}`,
      password: 'SecurePass1!',
      confirm_password: 'SecurePass1!',
    });
    assert.equal(userA.status, 201);
    assert.equal(userB.status, 201);

    // Activate both with generated codes inserted directly
    const { sha256, generateActivationCode } = require('../../src/utils/crypto');
    const { pool } = require('../../src/db');
    async function activate(token) {
      const code = generateActivationCode();
      await pool.query(`INSERT INTO activation_codes (code_hash, status) VALUES ($1, 'unused')`, [sha256(code)]);
      const res = await request(app)
        .post('/license/activate')
        .set('Authorization', `Bearer ${token}`)
        .send({ code });
      assert.equal(res.status, 200);
    }
    await activate(userA.body.access_token);
    await activate(userB.body.access_token);

    const created = await request(app)
      .post('/transactions')
      .set('Authorization', `Bearer ${userA.body.access_token}`)
      .send({
        provider: 'jazzcash',
        amount: 100,
        source_type: 'manual',
        review_status: 'manual',
        transaction_timestamp: new Date().toISOString(),
      });
    assert.equal(created.status, 201);

    const stolen = await request(app)
      .get(`/transactions/${created.body.id}`)
      .set('Authorization', `Bearer ${userB.body.access_token}`);
    assert.equal(stolen.status, 404);
  });
});
