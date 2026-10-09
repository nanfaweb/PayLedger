# Database migrations

Neon-compatible PostgreSQL SQL applied in filename order by `migrate.js`.

```bash
# from backend/ with DATABASE_URL set
npm run migrate
```

Tables: `users`, `refresh_sessions`, `password_reset_tokens`, `activation_codes`, `subscriptions`, `transactions`, `notification_events`, `user_provider_settings`.

Local development defaults to `postgresql://postgres:postgres@localhost:5432/payledger` (see `docker-compose.yml`).
