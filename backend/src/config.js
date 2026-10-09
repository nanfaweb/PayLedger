require('dotenv').config();

function required(name, fallback) {
  const value = process.env[name] ?? fallback;
  if (value === undefined || value === '') {
    throw new Error(`Missing required env var: ${name}`);
  }
  return value;
}

const config = {
  port: Number(process.env.PORT || 3000),
  nodeEnv: process.env.NODE_ENV || 'development',
  databaseUrl: required('DATABASE_URL', 'postgresql://postgres:postgres@localhost:5432/payledger'),
  jwtAccessSecret: required('JWT_ACCESS_SECRET', 'dev_access_secret_change_in_production_32'),
  jwtRefreshSecret: required('JWT_REFRESH_SECRET', 'dev_refresh_secret_change_in_production_32'),
  accessTokenTtlSeconds: Number(process.env.ACCESS_TOKEN_TTL_SECONDS || 900),
  refreshTokenTtlDays: Number(process.env.REFRESH_TOKEN_TTL_DAYS || 30),
  corsOrigin: process.env.CORS_ORIGIN || '*',
  rateLimitWindowMs: Number(process.env.RATE_LIMIT_WINDOW_MS || 900000),
  rateLimitMax: Number(process.env.RATE_LIMIT_MAX || 200),
  authRateLimitMax: Number(process.env.AUTH_RATE_LIMIT_MAX || 20),
  activationRateLimitMax: Number(process.env.ACTIVATION_RATE_LIMIT_MAX || 10),
};

module.exports = { config };
