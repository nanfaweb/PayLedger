const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const {
  hashPassword,
  verifyPassword,
  sha256,
  generateActivationCode,
} = require('../../src/utils/crypto');

describe('password hashing', () => {
  it('hashes and verifies with argon2id', async () => {
    const hash = await hashPassword('SecurePass1!');
    assert.ok(hash.startsWith('$argon2'));
    assert.equal(await verifyPassword(hash, 'SecurePass1!'), true);
    assert.equal(await verifyPassword(hash, 'wrong'), false);
  });
});

describe('activation codes', () => {
  it('generates unique PL- codes', () => {
    const a = generateActivationCode();
    const b = generateActivationCode();
    assert.match(a, /^PL-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}$/);
    assert.notEqual(a, b);
  });

  it('sha256 is deterministic', () => {
    assert.equal(sha256('abc'), sha256('abc'));
    assert.notEqual(sha256('abc'), sha256('abd'));
  });
});
