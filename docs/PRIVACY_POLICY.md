# PayLedger Privacy Policy (template)

**Last updated:** 2026-03-27  
**Operator:** [Your legal name / business name]  
**Contact:** [email]

This template must be customized and hosted at a public HTTPS URL before Play Store submission.

## What PayLedger does

PayLedger helps merchants organize incoming payment alerts from JazzCash, Easypaisa, SadaPay, and NayaPay by reading **notifications the user has already received on their device**, after the user grants Android notification access.

PayLedger does **not**:

- Access SMS, contacts, call logs, or accessibility services.
- Import historical wallet balances from payment providers.
- Independently verify that money reached a bank or wallet.
- Upload unrelated notifications from other apps.

## Data we collect

- Account: full name, phone number, password hash (never plaintext passwords).
- Licensing: activation/redemption metadata (code hashes only).
- Transactions: provider, amount, sender fields when present, timestamps, optional notes, original notification text for supported payment notifications the user chooses to process.
- Device session tokens for authentication.

## How we use data

- Provide the ledger, search, and PDF report features.
- Enforce subscription licensing.
- Support account deletion and security (session revocation, rate limiting).

## Notification access disclosure

Notification access is required solely to detect payment notifications from explicitly supported payment applications. Processing happens for eligible payment notifications only. Users can disable monitoring per provider in Settings.

## Retention and deletion

Users may delete their account in Settings. Deletion removes the user row and cascaded personal data (sessions, transactions, notification events, subscription row) from the PayLedger database.

## Security

- HTTPS for API traffic in production.
- Argon2id password hashing.
- Short-lived access tokens and rotatable refresh sessions.
- Server-side authorization scoped to the authenticated user.

## Contact

Questions about privacy: [email]
