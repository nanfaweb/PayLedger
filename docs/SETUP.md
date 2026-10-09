# PayLedger local setup

## Prerequisites

- Node.js 18+
- PostgreSQL 14+ (local), or Docker Desktop for `docker compose up -d`
- Android Studio Ladybug+ / SDK 35
- JDK 17 or 21

## 1. Database

### Option A — Docker

```bash
docker compose up -d
```

### Option B — Local PostgreSQL

Create database and user matching `backend/.env.example`:

```sql
CREATE DATABASE payledger;
```

## 2. Backend

```bash
cd backend
copy .env.example .env
npm install
npm run migrate
npm run dev
```

API: `http://localhost:3000/health`

## 3. Admin CLI

```bash
cd admin
npm install
node cli.js generate-codes --count 1000 --out exports/activation-codes.csv
```

Keep the CSV private. Only hashes are stored in PostgreSQL.

Password reset for a user:

```bash
node cli.js issue-password-reset --phone +923001234567
```

## 4. Android

1. Open `android/` in Android Studio.
2. Ensure `local.properties` contains `sdk.dir=...` (auto-created by Android Studio).
3. Emulator uses `http://10.0.2.2:3000/` (configured in `BuildConfig.API_BASE_URL`).
4. Physical device: set your PC LAN IP in `app/build.gradle.kts` debug `API_BASE_URL`, e.g. `http://192.168.1.10:3000/`.
5. Run the `app` configuration.

## 5. First user flow

1. Register with a Pakistani mobile number (`03XXXXXXXXX`).
2. Enter an activation code from the CSV.
3. Grant notification access in Settings.
4. Enable desired providers.
5. Receive a payment notification from a supported app (physical device) or add a manual transaction.

## Neon cutover (later)

1. Create a Neon project and copy the connection string.
2. Set `DATABASE_URL` in `backend/.env`.
3. Run `npm run migrate`.
4. Deploy the Express API behind HTTPS.
5. Set release `API_BASE_URL` to your HTTPS API.
