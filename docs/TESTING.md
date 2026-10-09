# Testing checklist

## Automated

```bash
cd backend
npm run test:unit
# Requires Postgres + migrated schema:
npm run test:integration
```

```bash
cd android
.\gradlew.bat test
```

## Backend manual

1. Register `03001234567` / password.
2. Login / refresh / logout.
3. Activate with unused code; second redeem of same code fails.
4. Create transaction for user A; confirm user B cannot read it (different token).
5. Expire subscription (set `expires_at` in DB) → transactions return 403.
6. Issue password reset via admin CLI and reset in API.

## Android emulator

1. Point API to `10.0.2.2:3000`.
2. Complete welcome → register → activate → dashboard.
3. Add manual transaction; search/filter; edit/delete.
4. Generate PDF and share sheet.
5. Switch language to Urdu; confirm RTL layout.
6. Settings toggles and logout / delete account.

## Physical device (required for notification listener)

1. Install debug APK.
2. Use LAN API URL in debug build.
3. Grant notification access.
4. Trigger real JazzCash/Easypaisa/SadaPay/NayaPay incoming payment notifications (or test notifications from those packages if available).
5. Confirm toast/snackbar “Payment recorded”, row appears, duplicate notification does not double-create when reference matches.
6. Toggle provider off and confirm notifications are ignored.

## Supported versions

- minSdk 26 (Android 8.0)
- targetSdk 35
- Test: Android 10, 12, 13, 14, 15 devices when possible
