const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const { daysRemaining, SUBSCRIPTION_DAYS } = require('../../src/services/licenseService');

describe('license helpers', () => {
  it('computes days remaining', () => {
    const inTenDays = new Date(Date.now() + 10 * 24 * 60 * 60 * 1000);
    const remaining = daysRemaining(inTenDays);
    assert.ok(remaining >= 9 && remaining <= 11);
  });

  it('subscription length is 365 days', () => {
    assert.equal(SUBSCRIPTION_DAYS, 365);
  });
});
