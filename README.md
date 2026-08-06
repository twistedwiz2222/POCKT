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

Payment apps make individual purchases frictionless, but they rarely show what each payment means for the rest of your month. POCKT turns the spends you add into something actionable:

> **Rs. 280 spent at Swiggy - Rs. 1,720 left - Rs. 132/day available**

There is no account, cloud service, advertising SDK, or financial-data upload. POCKT works locally on your phone.

## Features

- **Immediate budget feedback** after you add an expense
- **Monthly overview** with spent, remaining, days left, and safe daily allowance
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
| Local app storage | Save your budget and transactions on this phone |

POCKT does **not** request internet, notification access, Accessibility, SMS, contacts, storage, microphone, or location permissions. It never reads payment screens, PINs, or OTPs.

See [PRIVACY.md](PRIVACY.md) for the full privacy promise.

## How it works

```text
You add an expense
        |
        v
Local Room database
        |
        +-- budget dashboard
        +-- transaction history
        +-- safe daily allowance
```

The repo still contains parser experiments for payment notifications, but the public APK does not request notification access. That keeps the sideloaded build installable on devices where Play Protect blocks apps that can read sensitive notifications.

## Download an APK from GitHub Actions

1. Open the repository's **Actions** tab.
2. Select the latest successful **Android APK** run.
3. Under **Artifacts**, download `POCKT-release-apk`.
4. Extract the ZIP and install `POCKT-v0.5.0-release.apk` on your Android device.

Android may ask you to allow installation from your browser or file manager. This APK is signed for personal testing, but it is still not a Play Store release.

### If Android says "App not installed" or Play Protect blocks it

1. Uninstall any earlier POCKT build first if it appears in app settings. APKs produced before v0.5 requested sensitive notification access.
2. Download the APK again from the newest successful workflow, not the source-code ZIP.
3. Extract `POCKT-v0.5.0-release.apk` from the downloaded artifact ZIP before opening it.
4. Open the APK from Chrome Downloads or the phone's Files app. Avoid forwarding the APK through WhatsApp during testing.
5. Enable **Install unknown apps** for the browser or file manager you use to open the APK.
6. If Play Protect shows a warning, choose **More details** and then **Install anyway** only if you understand this is your own test build from your GitHub repo.
7. Confirm the device runs Android 6.0 or newer and has available storage.

From v0.5 onward, the APK does not declare notification-listener access, which avoids the Play Protect block shown on sideloaded builds.

You can also open **Android APK**, choose **Run workflow**, and start a fresh build manually.

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
3. Select the `app` run configuration.
4. Run it on your phone or emulator.
5. Add expenses manually after payments.

The repository's CI uses a pinned Gradle installation, so it does not depend on a checked-in wrapper binary.

## Tech stack

- Kotlin
- Jetpack Compose + Material 3
- Room + Kotlin Coroutines/Flow
- Local-first Android storage
- JUnit parser tests
- GitHub Actions for APK builds

## Project structure

```text
app/src/main/java/com/pockt/app/
+-- data/           Room entities, DAO, repository, budget state
+-- notifications/  Parser experiments kept out of the installable APK manifest
+-- ui/             Compose screens and view model
+-- ui/theme/       POCKT dark design system
+-- MainActivity.kt
+-- PocktApplication.kt
```

## Current status

POCKT is an early personal MVP. Before wider distribution it needs encrypted database handling, accessibility QA, signed release handling, and a compliant route for any automatic payment detection.

Contributions that add sanitized parser fixtures, improve transaction accuracy, or strengthen privacy are welcome. Never commit real financial notifications, account identifiers, UPI IDs, or personal transaction data.

## License

No open-source license has been selected yet. All rights are reserved unless a license is added later.
