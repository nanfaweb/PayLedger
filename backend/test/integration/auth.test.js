const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const request = require('supertest');

process.env.NODE_ENV = 'test';

let app;
let dbAvailable = false;

before(async () => {
  try {
    const { pool } = require('../../src/db');
    await pool.query('SELECT 1');
    dbAvailable = true;
    ({ createApp } = require('../../src/app'));
    app = createApp();
  } catch (err) {
    console.warn('Skipping integration tests: database unavailable —', err.message);
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

function skipIfNoDb() {
  if (!dbAvailable) {
    // node:test skip via return early with assert
    return true;
  }
  return false;
}

describe('auth API', () => {
  it('registers and logs in a user', async (t) => {
    if (skipIfNoDb()) return t.skip('database unavailable');

    const phone = `0300${String(Date.now()).slice(-7)}`;
    const reg = await request(app)
      .post('/auth/register')
      .send({
        full_name: 'Test Merchant',
        phone_number: phone,
        password: 'SecurePass1!',
        confirm_password: 'SecurePass1!',
      });

    assert.equal(reg.status, 201);
    assert.ok(reg.body.access_token);
    assert.equal(reg.body.user.phone_number, `+92${phone.slice(1)}`);

    const login = await request(app)
      .post('/auth/login')
      .send({ phone_number: phone, password: 'SecurePass1!' });
    assert.equal(login.status, 200);
    assert.ok(login.body.refresh_token);
  });

  it('rejects duplicate phone', async (t) => {
    if (skipIfNoDb()) return t.skip('database unavailable');

    const phone = `0301${String(Date.now()).slice(-7)}`;
    const body = {
      full_name: 'Dup User',
      phone_number: phone,
      password: 'SecurePass1!',
      confirm_password: 'SecurePass1!',
    };
    await request(app).post('/auth/register').send(body);
    const dup = await request(app).post('/auth/register').send(body);
    assert.equal(dup.status, 409);
  });
});
