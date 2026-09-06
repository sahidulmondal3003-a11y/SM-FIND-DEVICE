# SM Find Device
**Developed By Sahin**

Owner-authorized lost-Android-device recovery via SMS. The owner installs the app,
grants permissions, configures one or more trusted recovery numbers, and later — if
the phone is lost — sends an authorized SMS command from any of those numbers to get
a fresh location back by SMS, with a Google Maps link.

This is a real Kotlin + Jetpack Compose + Material 3 Android Studio project (not a
PWA or web wrapper). Open the root folder in Android Studio (Koala+ / AGP 8.5) to build.

## What's implemented

- **Multiple recovery numbers** (`data/RecoveryNumber.kt`, `storage/SecureConfigStore.kt`,
  `viewmodel/RecoveryNumbersViewModel.kt`, `ui/screens/RecoveryNumbersScreen.kt`):
  unlimited add/edit/enable-disable/delete, friendly labels, one Primary number,
  duplicate prevention via `security/PhoneNumberNormalizer.kt`, per-number Verify
  and Test Recovery, and a "No active recovery number is configured" warning when
  every number is disabled.
- **Two-factor SMS authorization** (`sms/RecoveryRequestValidator.kt`): sender must
  match an enabled+verified stored number *and* the message must exactly match an
  enabled command. Either check failing means silent ignore — no location disclosure.
- **15 configurable SMS command slots** (`data/RecoveryCommand.kt`,
  `ui/screens/SmsCommandsScreen.kt`), case-insensitive, exact-match only (no fuzzy
  matching), individually enable/disable/edit, with reset-to-default.
- **Real location acquisition** (`location/LocationAcquisitionEngine.kt`): fused
  provider, high-accuracy first, timeout-bounded, explicit last-known fallback that
  is always labeled as such, never a fabricated or mocked coordinate.
- **Per-request SMS reply to the same number that asked** (`sms/RecoverySmsSender.kt`,
  `sms/RecoverySmsReceiver.kt`) — a request from Number 2 is answered to Number 2 only.
- **Loop and duplicate prevention** (sections 38–39): outgoing replies carry a marker
  the receiver refuses to treat as a command; a SHA-256 request id keyed on
  sender+body+minute suppresses re-delivered SMS broadcasts; a cooldown blocks rapid-fire
  repeats while still allowing genuinely new requests through.
- **Foreground service only while a request is in flight** (`service/LocationRecoveryService.kt`)
  — no permanent background service, no continuous polling.
- **Encrypted local storage only** (`storage/SecureConfigStore.kt`, Android Keystore via
  `androidx.security.crypto`), excluded from auto-backup/device-transfer
  (`res/xml/data_extraction_rules.xml`). No server, no analytics, no ad SDKs.
- **Setup wizard** (`ui/screens/setup/SetupWizardScreen.kt`), **Dashboard** with a
  real, checklist-gated `PROTECTION ACTIVE` state (`data/ProtectionStateEvaluator.kt`),
  **Permissions** and **Settings/Privacy** screens with an explicit Disable Protection
  control.

## Deliberately not implemented / left as real, honest limitations

- No fuzzy/AI command matching (spec forbids it — false positives are a security risk).
- No attempt to defeat Android 14+/Play Store SMS-permission restrictions; the app
  tells the owner plainly if SMS auto-response isn't available on their build/distribution
  channel rather than working around it.
- No root, accessibility-service abuse, or hidden background persistence — background
  operation is best-effort within what the OS and the owner's own battery settings allow,
  and the Dashboard reports that honestly (Optimized / Restricted / Recommended setup).
- "Verify Number" is implemented as a local SMS round-trip confirmation (no backend),
  consistent with the "no cloud server required" requirement in the spec.

## Project layout

```
app/src/main/java/com/sahin/smfinddevice/
  data/          RecoveryNumber, RecoveryCommand, LocationResult, ProtectionStateEvaluator
  security/      PhoneNumberNormalizer
  storage/       SecureConfigStore (encrypted)
  sms/           RecoverySmsReceiver, RecoveryRequestValidator, RecoverySmsSender
  location/      LocationAcquisitionEngine
  permissions/   PermissionChecker
  service/       LocationRecoveryService (foreground), BootCompletedReceiver
  viewmodel/     DashboardViewModel, RecoveryNumbersViewModel
  ui/            MainActivity, screens/ (Dashboard, RecoveryNumbers, SmsCommands,
                 Permissions, Settings, setup/SetupWizardScreen), theme/
```

## Notes for whoever finishes wiring this up

- Set `PhoneNumberNormalizer`'s default country code from the SIM region at setup
  time instead of the hardcoded `"91"` default if you expect users outside India.
- `LocationRecoveryService.handleOutcome` currently doesn't persist the full last-known
  `LocationResult` to `SecureConfigStore.lastLocationResultJson` — wire that up if you
  want the Dashboard's "Last location accuracy" field to survive process death.
- SMS auto-send (`RECEIVE_SMS`/`SEND_SMS` as a non-default SMS app) is a restricted
  permission group on the Play Store; for Play distribution you'd apply for the
  "Device Manufacturer" / core-use exception or ship this build as a private/sideloaded
  APK, per section 44 of the spec.
