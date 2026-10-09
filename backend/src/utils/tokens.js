const jwt = require('jsonwebtoken');
const { config } = require('../config');
const { sha256, randomToken } = require('./crypto');

function signAccessToken(user) {
  return jwt.sign(
    {
      sub: user.id,
      phone: user.phone_number,
      typ: 'access',
    },
    config.jwtAccessSecret,
    { expiresIn: config.accessTokenTtlSeconds }
  );
}

function verifyAccessToken(token) {
  return jwt.verify(token, config.jwtAccessSecret);
}

function createRefreshTokenValue() {
  return randomToken(48);
}

function hashRefreshToken(token) {
  return sha256(token);
}

function refreshExpiryDate() {
  const d = new Date();
  d.setDate(d.getDate() + config.refreshTokenTtlDays);
  return d;
}

module.exports = {
  signAccessToken,
  verifyAccessToken,
  createRefreshTokenValue,
  hashRefreshToken,
  refreshExpiryDate,
};
