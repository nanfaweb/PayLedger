const express = require('express');
const { z } = require('zod');
const rateLimit = require('express-rate-limit');
const { config } = require('../config');
const { validate } = require('../middleware/validate');
const { requireAuth } = require('../middleware/auth');
const licenseService = require('../services/licenseService');

const router = express.Router();

const activationLimiter = rateLimit({
  windowMs: config.rateLimitWindowMs,
  max: config.activationRateLimitMax,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many activation attempts. Try again later.' },
});

const activateSchema = z.object({
  body: z.object({
    code: z.string().min(8).max(64),
  }),
});

router.get('/status', requireAuth, async (req, res, next) => {
  try {
    const status = await licenseService.getLicenseStatus(req.userId);
    res.json(status);
  } catch (err) {
    next(err);
  }
});

router.post('/activate', requireAuth, activationLimiter, validate(activateSchema), async (req, res, next) => {
  try {
    const status = await licenseService.activateCode(req.userId, req.body.code);
    res.json(status);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
