# PayLedger

Native Android payment ledger for Pakistani merchants. Detects incoming JazzCash, Easypaisa, SadaPay, and NayaPay notifications, stores transactions in your account, and exports PDF reports.

## Structure

```
android/    Kotlin + Jetpack Compose (Material 3) client
backend/    Node.js + Express API
database/   PostgreSQL migrations (local or Neon)
admin/      Developer CLI (activation codes, password resets)
docs/       Setup, privacy, Play Store, testing
```

## Quick start

See [docs/SETUP.md](docs/SETUP.md).

```bash
# Database (Docker)
docker compose up -d

# API
cd backend
copy .env.example .env
npm install
npm run migrate
npm run dev

# Activation codes (developer only)
cd ../admin
npm install
node cli.js generate-codes --count 1000 --out exports/codes.csv

# Android: open android/ in Android Studio and Run
```

## Security notes

- Passwords hashed with Argon2id.
- Activation codes stored as SHA-256 hashes; plaintext CSV is developer-only.
- Subscription validity is enforced on the server.
- The APK never contains database credentials.
- Notification access processes only supported payment apps.

## Honest limitation

PayLedger can only read notifications delivered to the device after the user grants notification access. It cannot pull historical wallet statements or prove bank settlement.

## Documentation

- [Setup](docs/SETUP.md)
- [Deployment / Neon](docs/DEPLOYMENT.md)
- [Privacy policy template](docs/PRIVACY_POLICY.md)
- [Play Store checklist](docs/PLAY_STORE.md)
- [Testing](docs/TESTING.md)

## License

Proprietary — all rights reserved by the project owner.
