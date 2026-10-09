const crypto = require('crypto');
const argon2 = require('argon2');

const ARGON2_OPTIONS = {
  type: argon2.argon2id,
  memoryCost: 19456,
  timeCost: 2,
  parallelism: 1,
};

async function hashPassword(password) {
  return argon2.hash(password, ARGON2_OPTIONS);
}

async function verifyPassword(hash, password) {
  try {
    return await argon2.verify(hash, password);
  } catch {
    return false;
  }
}

function sha256(value) {
  return crypto.createHash('sha256').update(value, 'utf8').digest('hex');
}

function randomToken(bytes = 32) {
  return crypto.randomBytes(bytes).toString('base64url');
}

/** Readable activation / reset codes: PL-XXXX-XXXX-XXXX-XXXX */
function generateActivationCode() {
  const part = () => crypto.randomBytes(3).toString('hex').toUpperCase().slice(0, 4);
  return `PL-${part()}-${part()}-${part()}-${part()}`;
}

function generateResetToken() {
  return `PL-RESET-${crypto.randomBytes(18).toString('base64url')}`;
}

module.exports = {
  hashPassword,
  verifyPassword,
  sha256,
  randomToken,
  generateActivationCode,
  generateResetToken,
};
