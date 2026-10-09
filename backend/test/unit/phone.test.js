const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const { normalizePakistaniPhone, isValidPakistaniPhone } = require('../../src/utils/phone');

describe('normalizePakistaniPhone', () => {
  it('normalizes local 03xx format', () => {
    assert.equal(normalizePakistaniPhone('03001234567'), '+923001234567');
  });

  it('normalizes +92 format', () => {
    assert.equal(normalizePakistaniPhone('+92 300 1234567'), '+923001234567');
  });

  it('normalizes 92 without plus', () => {
    assert.equal(normalizePakistaniPhone('923001234567'), '+923001234567');
  });

  it('rejects landlines and short numbers', () => {
    assert.equal(normalizePakistaniPhone('0211234567'), null);
    assert.equal(normalizePakistaniPhone('123'), null);
    assert.equal(isValidPakistaniPhone('abc'), false);
  });
});
