# PayLedger Admin CLI

Developer-only utilities. Do not ship this to end users.

```bash
npm install
# Uses backend/.env DATABASE_URL
node cli.js generate-codes --count 1000 --out exports/activation-codes.csv
node cli.js list-codes --status unused
node cli.js revoke-code --id <uuid>
node cli.js inspect-user --phone +923001234567
node cli.js issue-password-reset --phone +923001234567
```

Activation code plaintext exists only in the CSV export. PostgreSQL stores SHA-256 hashes.

Password reset tokens expire in 30 minutes and can be used once in the Android app.
