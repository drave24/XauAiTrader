# Build the APK without a PC

The ChatGPT workspace cannot compile Android APKs because it does not have the Android SDK/Gradle toolchain. This project includes a GitHub Actions workflow that builds the APK in the cloud.

## Steps

1. Create a GitHub account if you do not already have one.
2. Create a new empty repository, for example `XauAiTrader`.
3. Upload the **contents** of this project into the repository (not just the ZIP file).
4. Open the repository's **Actions** tab.
5. Select **Build XAU AI Trader APK**.
6. Tap **Run workflow**.
7. Wait for the green checkmark.
8. Open that workflow run and scroll to **Artifacts**.
9. Download `XauAiTrader-debug-apk`.
10. Extract it and open `app-debug.apk` on your Android phone.
11. If Android asks, allow your browser/file manager to install unknown apps, then install.

The APK is a debug build and is suitable for testing. It is not yet connected to a live MT5 backend; the app's current Start/Stop/Refresh behavior is placeholder UI.
