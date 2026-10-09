const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');
const { config } = require('./config');
const { notFoundHandler, errorHandler } = require('./middleware/error');

const authRoutes = require('./routes/auth');
const licenseRoutes = require('./routes/license');
const transactionRoutes = require('./routes/transactions');
const meRoutes = require('./routes/me');

function createApp() {
  const app = express();

  app.set('trust proxy', 1);
  app.use(helmet());
  app.use(
    cors({
      origin: config.corsOrigin === '*' ? true : config.corsOrigin,
      credentials: true,
    })
  );
  app.use(express.json({ limit: '256kb' }));

  morgan.token('safe-body', (req) => {
    if (!req.body || typeof req.body !== 'object') return '';
    const clone = { ...req.body };
    for (const key of Object.keys(clone)) {
      if (/password|token|code|secret/i.test(key)) clone[key] = '[REDACTED]';
    }
    return JSON.stringify(clone);
  });

  if (config.nodeEnv !== 'test') {
    app.use(morgan(':method :url :status :response-time ms'));
  }

  app.use(
    rateLimit({
      windowMs: config.rateLimitWindowMs,
      max: config.rateLimitMax,
      standardHeaders: true,
      legacyHeaders: false,
    })
  );

  app.get('/health', (_req, res) => {
    res.json({ ok: true, service: 'payledger-api' });
  });

  app.use('/auth', authRoutes);
  app.use('/license', licenseRoutes);
  app.use('/transactions', transactionRoutes);
  app.use('/me', meRoutes);

  app.use(notFoundHandler);
  app.use(errorHandler);

  return app;
}

module.exports = { createApp };
