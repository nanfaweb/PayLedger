const express = require('express');
const { z } = require('zod');
const { validate } = require('../middleware/validate');
const { requireAuth } = require('../middleware/auth');
const authService = require('../services/authService');
const licenseService = require('../services/licenseService');
const txn = require('../services/transactionService');

const router = express.Router();

router.use(requireAuth);

const profileSchema = z.object({
  body: z.object({
    full_name: z.string().min(2).max(120).optional(),
    preferred_language: z.enum(['en', 'ur']).optional(),
  }),
});

const providerSchema = z.object({
  body: z.object({
    provider: z.enum(['jazzcash', 'easypaisa', 'sadapay', 'nayapay']),
    monitoring_enabled: z.boolean(),
  }),
});

router.get('/', async (req, res, next) => {
  try {
    const license = await licenseService.getLicenseStatus(req.userId);
    const providers = await txn.getProviderSettings(req.userId);
    const lastNotification = await txn.getLastNotificationStatus(req.userId);
    res.json({
      user: authService.publicUser(req.user),
      license,
      providers,
      last_notification: lastNotification,
    });
  } catch (err) {
    next(err);
  }
});

router.patch('/', validate(profileSchema), async (req, res, next) => {
  try {
    const user = await authService.updateProfile(req.userId, {
      fullName: req.body.full_name,
      preferredLanguage: req.body.preferred_language,
    });
    res.json({ user: authService.publicUser(user) });
  } catch (err) {
    next(err);
  }
});

router.delete('/', async (req, res, next) => {
  try {
    await authService.deleteAccount(req.userId);
    res.json({ ok: true, message: 'Account and associated data deleted.' });
  } catch (err) {
    next(err);
  }
});

router.get('/providers', async (req, res, next) => {
  try {
    res.json({ providers: await txn.getProviderSettings(req.userId) });
  } catch (err) {
    next(err);
  }
});

router.put('/providers', validate(providerSchema), async (req, res, next) => {
  try {
    const providers = await txn.setProviderSetting(
      req.userId,
      req.body.provider,
      req.body.monitoring_enabled
    );
    res.json({ providers });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
