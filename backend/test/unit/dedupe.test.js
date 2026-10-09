const { describe, it } = require('node:test');
const assert = require('node:assert/strict');

/**
 * Mirror of Android dedupe semantics for documentation/tests.
 * Prefer transaction reference; otherwise include a time bucket so identical
 * sender+amount pairs are not treated as automatic duplicates.
 */
function dedupeKey({ provider, reference, amount, senderIdentifier, senderName, originalText, postedAtMs }) {
  if (reference) return `${provider}|ref|${String(reference).trim().toUpperCase()}`;
  const bucket = Math.floor(postedAtMs / 120000);
  const basis = [provider, amount ?? '', senderIdentifier ?? '', senderName ?? '', String(originalText).slice(0, 120), bucket].join('|');
  let hash = 0;
  for (let i = 0; i < basis.length; i++) hash = (Math.imul(31, hash) + basis.charCodeAt(i)) | 0;
  return `h|${(hash >>> 0).toString(16)}`;
}

describe('deduplication strategy', () => {
  it('collapses same reference', () => {
    const a = dedupeKey({ provider: 'jazzcash', reference: 'ABC', amount: 1, postedAtMs: 1, originalText: 'x' });
    const b = dedupeKey({ provider: 'jazzcash', reference: 'abc', amount: 9, postedAtMs: 999999, originalText: 'y' });
    assert.equal(a, b);
  });

  it('does not treat same sender+amount as duplicate across buckets', () => {
    const a = dedupeKey({
      provider: 'easypaisa',
      amount: 100,
      senderIdentifier: '03001112222',
      senderName: 'Ali',
      originalText: 'recv',
      postedAtMs: 0,
    });
    const b = dedupeKey({
      provider: 'easypaisa',
      amount: 100,
      senderIdentifier: '03001112222',
      senderName: 'Ali',
      originalText: 'recv',
      postedAtMs: 200000,
    });
    assert.notEqual(a, b);
  });
});
