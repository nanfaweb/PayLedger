# Backend deployment

## Security checklist

- Use HTTPS only in production (`NODE_ENV=production`).
- Set long random `JWT_ACCESS_SECRET` and `JWT_REFRESH_SECRET`.
- Never commit `.env`, activation CSV files, or keystores.
- Restrict CORS to your app / admin origins.
- Keep rate limits enabled for `/auth/*` and `/license/activate`.
- Database credentials stay on the server only — never in the APK.

## Suggested hosts

Railway, Render, Fly.io, or any Node host with a public HTTPS URL.

Example start command:

```bash
npm install --prefix backend
npm run migrate --prefix backend
npm start --prefix backend
```

## Neon

1. Create Neon Postgres.
2. Paste the pooled connection string into `DATABASE_URL`.
3. Run migrations once.
4. Generate activation codes with the admin CLI pointed at Neon:

```bash
# backend/.env DATABASE_URL=<neon url>
node admin/cli.js generate-codes --count 1000 --out admin/exports/codes.csv
```

## Health check

`GET /health` → `{ "ok": true, "service": "payledger-api" }`
