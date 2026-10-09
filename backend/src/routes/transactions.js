const express = require('express');
const { z } = require('zod');
const { validate } = require('../middleware/validate');
const { requireAuth } = require('../middleware/auth');
const { requireActiveSubscription } = require('../middleware/subscription');
const txn = require('../services/transactionService');

const router = express.Router();

router.use(requireAuth, requireActiveSubscription);

const createSchema = z.object({
  body: z.object({
    provider: z.enum(['jazzcash', 'easypaisa', 'sadapay', 'nayapay', 'other']),
    sender_name: z.string().max(200).optional().nullable(),
    sender_identifier: z.string().max(64).optional().nullable(),
    amount: z.number().nonnegative(),
    currency: z.string().max(8).optional(),
    transaction_reference: z.string().max(128).optional().nullable(),
    transaction_timestamp: z.string().datetime().optional(),
    received_at: z.string().datetime().optional(),
    transaction_direction: z.enum(['incoming', 'outgoing']).optional(),
    original_notification: z.string().max(4000).optional().nullable(),
    source_type: z.enum(['notification', 'manual']).optional(),
    notes: z.string().max(2000).optional().nullable(),
    review_status: z.enum(['confirmed', 'needs_review', 'manual']).optional(),
    deduplication_key: z.string().max(256).optional().nullable(),
  }),
});

const updateSchema = z.object({
  params: z.object({ id: z.string().uuid() }),
  body: z.object({
    sender_name: z.string().max(200).optional().nullable(),
    sender_identifier: z.string().max(64).optional().nullable(),
    amount: z.number().nonnegative().optional(),
    notes: z.string().max(2000).optional().nullable(),
    review_status: z.enum(['confirmed', 'needs_review', 'manual']).optional(),
    transaction_reference: z.string().max(128).optional().nullable(),
    transaction_direction: z.enum(['incoming', 'outgoing']).optional(),
    provider: z.enum(['jazzcash', 'easypaisa', 'sadapay', 'nayapay', 'other']).optional(),
  }),
});

router.get('/summary', async (req, res, next) => {
  try {
    const summary = await txn.getSummary(req.userId);
    res.json(summary);
  } catch (err) {
    next(err);
  }
});

router.get('/', async (req, res, next) => {
  try {
    const result = await txn.searchTransactions(req.userId, {
      q: req.query.q,
      sender_name: req.query.sender_name,
      sender_identifier: req.query.sender_identifier,
      amount: req.query.amount,
      amount_min: req.query.amount_min,
      amount_max: req.query.amount_max,
      reference: req.query.reference,
      provider: req.query.provider,
      direction: req.query.direction,
      review_status: req.query.review_status,
      date_from: req.query.date_from,
      date_to: req.query.date_to,
      sort: req.query.sort,
      page: req.query.page,
      page_size: req.query.page_size,
    });
    res.json(result);
  } catch (err) {
    next(err);
  }
});

router.post('/', validate(createSchema), async (req, res, next) => {
  try {
    if (req.body.source_type === 'notification' && req.body.deduplication_key) {
      await txn.recordNotificationEvent(req.userId, {
        provider: req.body.provider,
        deduplication_key: req.body.deduplication_key,
        processing_status: req.body.review_status === 'needs_review' ? 'needs_review' : 'processed',
        title: null,
        text_body: req.body.original_notification,
      });
    }
    const created = await txn.createTransaction(req.userId, req.body);
    res.status(201).json(created);
  } catch (err) {
    next(err);
  }
});

router.get('/:id', async (req, res, next) => {
  try {
    const item = await txn.getTransaction(req.userId, req.params.id);
    res.json(item);
  } catch (err) {
    next(err);
  }
});

router.patch('/:id', validate(updateSchema), async (req, res, next) => {
  try {
    const item = await txn.updateTransaction(req.userId, req.params.id, req.body);
    res.json(item);
  } catch (err) {
    next(err);
  }
});

router.delete('/:id', async (req, res, next) => {
  try {
    await txn.deleteTransaction(req.userId, req.params.id);
    res.json({ ok: true });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
