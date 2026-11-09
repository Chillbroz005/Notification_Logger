# Notif Logger (Notification-Logger)

An Android application that logs and archives all incoming system notifications locally on your device with search, filter, export, and remote screenshot capabilities.

---

## 🚀 Features

- **Local Notification Logging**: Captures app name, sender title, message text, and timestamp in a local Room SQLite database.
- **Remote `/screenshot` Command**:
  - Automatically captures the device screen when a notification containing `/screenshot` arrives (e.g. from **Telegram** or any messaging app).
  - Uses the **native device format** via Android Accessibility Service (`GLOBAL_ACTION_TAKE_SCREENSHOT`), saving the screenshot to your device's standard Screenshots directory.
  - On supported Android versions, also attaches the screenshot preview directly to the notification item in the database.
- **In-App Fullscreen Screenshot Viewer**: Tap any image thumbnail in the list to open and view the full-size screenshot.
- **Search & Filter**: Search notifications by app, title, or body text, filter by date, or group by app/date.
- **Export**: Export notification history to a formatted text file.
- **Automated Release on Update**: Every push to the `main` branch automatically triggers GitHub Actions to build and publish a new GitHub Release with the compiled APK ready to download.

---

## 📥 Downloads

Get the latest compiled APK from the **[Releases](https://github.com/Chillbroz005/Notification_Logger/releases)** page:
- `NotifLogger-latest.apk` (Always points to the latest release)
- `NotifLogger-v1.0.<build>.apk` (Numbered releases)

---

## 📱 Permissions Setup

For full functionality, enable the two permissions in Android Settings:

1. **Notification Access**:
   - Tap **Grant Notification Access** in the app.
   - Toggle **Notif Logger** to ON to allow reading notifications.

2. **Accessibility Access (for `/screenshot`)**:
   - Tap **Grant Accessibility Access (for /screenshot)** in the app.
   - Toggle **Notif Logger Screenshot Service** to ON.
   - This allows the app to trigger native device screenshots when `/screenshot` is received remotely via Telegram.

---

## 🛠️ Building Locally

```bash
git clone https://github.com/Chillbroz005/Notification_Logger.git
cd Notification_Logger
./gradlew assembleDebug
```
The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.
