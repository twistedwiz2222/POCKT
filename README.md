<div align="center">

# POCKT

### Know what every payment costs you.

A private, offline-first Android spending companion that turns payment confirmations into immediate budget awareness.

![Android](https://img.shields.io/badge/Android-8.0%2B-61E7B6?style=flat-square&logo=android&logoColor=080A0D)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-9CB8FF?style=flat-square&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-61E7B6?style=flat-square)
![Privacy](https://img.shields.io/badge/Data-on--device-181D23?style=flat-square)

</div>

## Why POCKT exists

Payment apps make individual purchases frictionless, but they rarely show what each payment means for the rest of your month. POCKT observes successful payment notifications—with explicit Android permission—and translates them into something actionable:

> **₹280 spent at Swiggy · ₹1,720 left · ₹132/day available**

There is no account, cloud service, advertising SDK, or financial-data upload. POCKT works locally on your phone.

## Features

- **Immediate budget feedback** after a recognized UPI payment
- **Monthly overview** with spent, remaining, days left, and safe daily allowance
- **Automatic local detection** for Google Pay, Paytm, PhonePe, and BHIM
- **Manual expenses** for cash or missed transactions
- **Deterministic parsing** that ignores failed and pending payments
- **Duplicate protection** using one-way SHA-256 fingerprints
- **Local transaction history** backed by Room
- **Dark, focused interface** built with Material 3 and Jetpack Compose
- **Complete deletion** of local POCKT data from Settings

## Privacy by design

POCKT requests only the capabilities necessary for its core experience:

| Capability | Why it is used |
| --- | --- |
| Notification-listener access | Detect supported payment confirmations after user approval |
| Post notifications | Show immediate budget feedback on Android 13+ |

POCKT does **not** request internet, Accessibility, SMS, contacts, storage, microphone, or location permissions. It never reads payment screens, PINs, or OTPs. Raw notification text is parsed in memory and is not persisted.

See [PRIVACY.md](PRIVACY.md) for the full privacy promise.

## How it works

```text
Payment app posts a success notification
                 │
                 ▼
Android NotificationListenerService
                 │ package allow-list
                 ▼
PaymentParser
  amount · merchant · direction · category
                 │
                 ├── duplicate check
                 ▼
Local Room database
                 │
                 ├── Compose dashboard
                 └── immediate budget notification
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
3. Under **Artifacts**, download `POCKT-debug-apk`.
4. Extract the ZIP and install `app-debug.apk` on your Android device.

Android may ask you to allow installation from your browser or file manager. Debug APKs are intended for personal testing and are not Play Store releases.

You can also open **Android APK**, choose **Run workflow**, and start a fresh build manually.

## Build locally

### Requirements

- Android Studio with Android SDK 36
- JDK 17
- Android 8.0/API 26 or newer device or emulator

### Steps

1. Clone and open the project:

   ```bash
   git clone https://github.com/twistedwiz2222/POCKT.git
   cd POCKT
   ```

2. Open the folder in Android Studio and allow Gradle to sync.
3. Select the `app` run configuration.
4. Run it on your phone or emulator.
5. During onboarding, tap **Enable notification access** and approve **POCKT payment detection**.
6. On Android 13+, approve POCKT notifications.

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
├── data/           Room entities, DAO, repository, budget state
├── notifications/  Payment parser and Android listener service
├── ui/             Compose screens and view model
├── ui/theme/       POCKT dark design system
├── MainActivity.kt
└── PocktApplication.kt
```

## Current status

POCKT is an early personal MVP. Before wider distribution it needs notification-format testing across device vendors and payment-app versions, encrypted database handling, accessibility QA, signed release builds, and Play policy review.

Contributions that add sanitized parser fixtures, improve transaction accuracy, or strengthen privacy are welcome. Never commit real financial notifications, account identifiers, UPI IDs, or personal transaction data.

## License

No open-source license has been selected yet. All rights are reserved unless a license is added later.
