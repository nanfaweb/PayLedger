function notFoundHandler(req, res) {
  res.status(404).json({ error: 'Not found' });
}

function errorHandler(err, req, res, _next) {
  if (err.name === 'ZodError') {
    return res.status(400).json({
      error: 'Validation failed',
      details: err.errors.map((e) => ({ path: e.path.join('.'), message: e.message })),
    });
  }

  if (err.status && err.message) {
    return res.status(err.status).json({ error: err.message, code: err.code });
  }

  // Avoid leaking internals; never log passwords / codes
  console.error('[error]', err.message);
  return res.status(500).json({ error: 'Internal server error' });
}

function createHttpError(status, message, code) {
  const err = new Error(message);
  err.status = status;
  err.code = code;
  return err;
}

module.exports = { notFoundHandler, errorHandler, createHttpError };
