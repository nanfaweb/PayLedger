const { createApp } = require('./app');
const { config } = require('./config');
const { pool } = require('./db');

async function start() {
  // Verify DB connectivity early
  await pool.query('SELECT 1');
  const app = createApp();
  app.listen(config.port, () => {
    console.log(`PayLedger API listening on port ${config.port}`);
  });
}

start().catch((err) => {
  console.error('Failed to start server:', err.message);
  process.exit(1);
});
