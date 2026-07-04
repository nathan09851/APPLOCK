# AppLock 🔒

A modern, lightweight, and offline-first Android App Locker built with **Kotlin, Jetpack Compose, and Material 3**. Designed specifically to run smoothly on devices like the **Samsung A35 (Exynos 1380)**.

---

## 📲 Download & Install APK

If you are browsing this repository from your phone, you can download the pre-compiled APK directly:

1. Click on **[AppLock-debug.apk](https://github.com/nathan09851/APPLOCK/blob/main/AppLock-debug.apk)** in the file list above.
2. Tap the **Download / View Raw** button.
3. Once downloaded, open the APK to install it.

---

## 🛡️ Play Protect Warning (Bypass Guide)
Because this app is self-compiled and requests sensitive permissions (Accessibility Service to monitor when locked apps open), Google Play Protect will flag it as an unverified developer app.

### How to Bypass:
1. When the *"Blocked by Play Protect"* popup appears, tap **"More details"** (or the downward arrow).
2. Tap **"Install anyway"** to finish installation.
3. Alternatively, temporarily disable **"Scan apps with Play Protect"** in Google Play Store Settings -> Play Protect before installing.

---

## ✨ Features
- **Secure PIN Storage**: PIN is salted and hashed using SHA-256 inside Jetpack Security's `EncryptedSharedPreferences`.
- **Biometric Prompt**: Fingerprint or face recognition authentication with automatic fallback to your custom PIN pad.
- **Accessibility Service**: Reacts instantly to app window changes with zero background battery polling.
- **Re-lock Policies**: Custom timers (Immediately, 10 Seconds, 1 Minute).
- **Brute-Force Protection**: 30-second lockout penalty after 5 incorrect attempts.
- **Privacy First**: No internet permission requested or used. No ads, trackers, or telemetry.
