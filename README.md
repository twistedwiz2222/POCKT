<div align="center">

<img src="assets/branding/pockt-logo.png" alt="POCKT logo" width="160" />

# POCKT

### Know what every payment costs you.

A private, offline-first Android spending companion that turns payment confirmations into immediate budget awareness.

![Android](https://img.shields.io/badge/Android-6.0%2B-61E7B6?style=flat-square&logo=android&logoColor=080A0D)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-9CB8FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-61E7B6?style=flat-square)
![Privacy](https://img.shields.io/badge/Data-on--device-181D23?style=flat-square)

</div>

## Why POCKT exists

Payment apps make individual purchases frictionless, but they rarely show what each payment means for the rest of your month. POCKT watches supported successful payment notifications, stores the spend locally, and sends immediate budget feedback:

> **Rs. 280 spent at Swiggy - Rs. 1,720 left - Rs. 132/day available**

There is no account, cloud service, advertising SDK, or financial-data upload. POCKT works locally on your phone.

## Builds

POCKT now ships two APK flavors from GitHub Actions:

| APK | What it does | Play Protect risk |
| --- | --- | --- |
| `POCKT-FULL-v0.9.0-detector.apk` | Detects supported payment notifications and sends POCKT budget notifications | May be blocked when sideloaded because Android treats notification-listener apps as sensitive |
| `POCKT-SAFE-v0.9.0-manual.apk` | Manual expense tracking only | Lower risk, but not the core POCKT experience |

Use the **full detector** APK if you want the real automatic POCKT behavior.

## Features

- **Automatic local detection** for Google Pay, Paytm, PhonePe, and BHIM in the full build
- **Immediate budget notifications** after a recognized UPI payment in the full build
- **Manual expenses** for cash, missed transactions, or the safe build
- **Monthly overview** with spent, remaining, days left, and safe daily allowance
- **Deterministic parsing** that ignores failed and pending payments
- **Duplicate protection** using one-way SHA-256 fingerprints
- **Local transaction history** backed by Room
- **Dark, focused interface** built with Material 3 and Jetpack Compose
- **Complete deletion** of local POCKT data from Settings

## Privacy by design

The full detector build requests only these Android capabilities:

| Capability | Why it is used |
| --- | --- |
| Notification-listener access | Detect supported payment confirmations after user approval |
| Post notifications | Show immediate POCKT budget feedback on Android 13+ |

POCKT does **not** request internet, Accessibility, SMS, contacts, storage, microphone, or location permissions. It never reads payment screens, PINs, or OTPs. Raw notification text is parsed in memory and is not persisted.

See [PRIVACY.md](PRIVACY.md) for the full privacy promise.

## How it works

```text
Payment app posts a success notification
                 |
                 v
Android NotificationListenerService
                 | package allow-list
                 v
PaymentParser
  amount - merchant - direction - category
                 |
                 +-- duplicate check
                 v
Local Room database
                 |
                 +-- Compose dashboard
                 +-- immediate budget notification
```

POCKT currently recognizes these Android packages:

| App | Package |
| --- | --- |
| Google Pay | `com.google.android.apps.nbu.paisa.user` |
| Paytm | `net.one97.paytm` |
| PhonePe | `com.phonepe.app` |
| BHIM | `in.org.npci.upiapp` |

Payment apps can change their notification wording. POCKT deliberately ignores uncertain formats instead of risking an incorrect expense.

## Download an APK from GitHub Actions

1. Open the repository's **Actions** tab.
2. Select the latest successful **Android APK** run.
3. Under **Artifacts**, download `POCKT-release-apks`.
4. Extract the ZIP.
5. Install `POCKT-FULL-v0.9.0-detector.apk` for automatic payment detection.

Android may ask you to allow installation from your browser or file manager. This APK is signed for personal testing, but it is still not a Play Store release.

### If Play Protect blocks the full APK

That block is caused by Android treating sideloaded notification-listener apps as sensitive. The full build needs that access to shadow GPay, Paytm, PhonePe, and BHIM notifications.

Practical options:

- Install with ADB from a computer: `adb install POCKT-FULL-v0.9.0-detector.apk`
- Temporarily turn off Play Protect scanning, install POCKT, then turn scanning back on
- Use Play Console internal testing later, which is the cleaner route for testing sensitive-permission APKs
- Install `POCKT-SAFE-v0.9.0-manual.apk` only as a fallback

Do not install through WhatsApp during testing. Download from GitHub Actions and open the APK from Chrome Downloads or the phone's Files app.

## Build locally

### Requirements

- Android Studio with Android SDK 36
- JDK 17
- Android 6.0/API 23 or newer device or emulator

### Steps

1. Clone and open the project:

   ```bash
   git clone https://github.com/twistedwiz2222/POCKT.git
   cd POCKT
   ```

2. Open the folder in Android Studio and allow Gradle to sync.
3. Build the full detector APK:

   ```bash
   gradle assembleFullRelease
   ```

4. During onboarding, tap **Enable notification access** and approve **POCKT payment detection**.
5. On Android 13+, approve POCKT notifications.

The repository's CI uses a pinned Gradle installation, so it does not depend on a checked-in wrapper binary.

## Tech stack

- Kotlin
- Jetpack Compose + Material 3
- Room + Kotlin Coroutines/Flow
- Android `NotificationListenerService`
- JUnit parser tests
- GitHub Actions for APK builds

## Project structure

```text
app/src/main/java/com/pockt/app/
+-- data/           Room entities, DAO, repository, budget state
+-- notifications/  Payment parser and Android listener service
+-- ui/             Compose screens and view model
+-- ui/theme/       POCKT dark design system
+-- MainActivity.kt
+-- PocktApplication.kt
```

## Current status

POCKT is an early personal MVP. Before wider distribution it needs notification-format testing across device vendors and payment-app versions, encrypted database handling, accessibility QA, signed release handling, and Play policy review.

Contributions that add sanitized parser fixtures, improve transaction accuracy, or strengthen privacy are welcome. Never commit real financial notifications, account identifiers, UPI IDs, or personal transaction data.

## License

No open-source license has been selected yet. All rights are reserved unless a license is added later.
