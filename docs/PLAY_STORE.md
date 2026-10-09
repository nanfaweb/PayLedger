# Google Play release and compliance checklist

PayLedger uses `NotificationListenerService`. Google reviews this permission carefully. Approval is **not guaranteed**. Implement honest disclosures and a clear core use case.

## App identity

| Field | Value |
|-------|-------|
| Application ID | `com.payledger.app` |
| Name | PayLedger |
| Category | Finance / Business |
| Default language | English (+ Urdu) |

## Build signed AAB

1. Create a keystore (keep offline backups; never commit):

```bash
keytool -genkey -v -keystore payledger-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias payledger
```

2. Configure signing in `android/app/build.gradle.kts` or `keystore.properties` (local only).

3. Set release `API_BASE_URL` to your HTTPS API.

4. Build:

```bash
cd android
.\gradlew.bat bundleRelease
```

Output: `android/app/build/outputs/bundle/release/app-release.aab`

Debug APK for private testing:

```bash
.\gradlew.bat assembleDebug
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`

## Store listing (draft)

**Short description:**  
Track JazzCash, Easypaisa, SadaPay & NayaPay payment notifications in one simple ledger.

**Full description:**  
PayLedger helps Pakistani shopkeepers and freelancers organize incoming mobile-wallet payment alerts. After you grant notification access, PayLedger reads payment notifications from supported apps, extracts amounts and sender details when available, and stores them in your private account. Search history, fix misreads, and export weekly or monthly PDF reports. Access requires an activation code and a one-year subscription.

**Important:** PayLedger reads live device notifications only. It cannot fetch historical payments from wallet providers and notification text is not bank proof.

## Data Safety form (guidance)

Declare:

- Personal info: name, phone number
- Financial info: transaction amounts / payment history derived from notifications
- App activity: app interactions
- Collected and encrypted in transit
- Account deletion available
- Purpose: app functionality

## Notification access declaration

In Play Console permission declarations, explain:

- Core feature: organize incoming payment notifications from named wallet apps.
- Only those packages are processed.
- User-initiated grant via system settings.
- No accessibility / SMS workaround.

## Graphics

- App icon: 512×512
- Feature graphic: 1024×500
- Phone screenshots: at least 2 (dashboard, transactions)
- Optional tablet screenshots

## Policy review notes

- Do not claim bank-grade verification.
- Do not request unrelated sensitive permissions.
- Host privacy policy URL and link it in the app listing and in-app Settings/privacy text.
- Test on Android 8–15 physical devices before production rollout.
