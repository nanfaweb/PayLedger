const express = require('express');
const { z } = require('zod');
const rateLimit = require('express-rate-limit');
const { config } = require('../config');
const { validate } = require('../middleware/validate');
const { requireAuth } = require('../middleware/auth');
const authService = require('../services/authService');

const router = express.Router();

const authLimiter = rateLimit({
  windowMs: config.rateLimitWindowMs,
  max: config.authRateLimitMax,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many attempts. Try again later.' },
});

const registerSchema = z.object({
  body: z.object({
    full_name: z.string().min(2).max(120),
    phone_number: z.string().min(10).max(20),
    password: z.string().min(8).max(128),
    confirm_password: z.string().min(8).max(128),
  }).refine((d) => d.password === d.confirm_password, {
    message: 'Passwords do not match',
    path: ['confirm_password'],
  }),
});

const loginSchema = z.object({
  body: z.object({
    phone_number: z.string().min(10).max(20),
    password: z.string().min(1).max(128),
  }),
});

const refreshSchema = z.object({
  body: z.object({
    refresh_token: z.string().min(10),
  }),
});

const resetSchema = z.object({
  body: z.object({
    phone_number: z.string().min(10).max(20),
    reset_token: z.string().min(10),
    new_password: z.string().min(8).max(128),
    confirm_password: z.string().min(8).max(128),
  }).refine((d) => d.new_password === d.confirm_password, {
    message: 'Passwords do not match',
    path: ['confirm_password'],
  }),
});

router.post('/register', authLimiter, validate(registerSchema), async (req, res, next) => {
  try {
    const tokens = await authService.registerAndLogin(
      {
        fullName: req.body.full_name,
        phoneNumber: req.body.phone_number,
        password: req.body.password,
      },
      req.headers['user-agent']
    );
    res.status(201).json(tokens);
  } catch (err) {
    next(err);
  }
});

router.post('/login', authLimiter, validate(loginSchema), async (req, res, next) => {
  try {
    const tokens = await authService.login({
      phoneNumber: req.body.phone_number,
      password: req.body.password,
      userAgent: req.headers['user-agent'],
    });
    res.json(tokens);
  } catch (err) {
    next(err);
  }
});

router.post('/refresh', validate(refreshSchema), async (req, res, next) => {
  try {
    const tokens = await authService.refresh(req.body.refresh_token, req.headers['user-agent']);
    res.json(tokens);
  } catch (err) {
    next(err);
  }
});

router.post('/logout', validate(refreshSchema), async (req, res, next) => {
  try {
    await authService.logout(req.body.refresh_token);
    res.json({ ok: true });
  } catch (err) {
    next(err);
  }
});

router.post('/reset-password', authLimiter, validate(resetSchema), async (req, res, next) => {
  try {
    await authService.resetPasswordWithToken({
      phoneNumber: req.body.phone_number,
      resetToken: req.body.reset_token,
      newPassword: req.body.new_password,
    });
    res.json({ ok: true, message: 'Password updated. Please log in.' });
  } catch (err) {
    next(err);
  }
});

router.get('/me', requireAuth, async (req, res) => {
  res.json({ user: authService.publicUser(req.user) });
});

module.exports = router;
