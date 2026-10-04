# XAU AI Trader — Android project

This is the Android control/monitoring app for the XAU/USD M5 AI MT5 bot.

## What it does

- Dashboard for XAU/USD M5
- AI signal display
- Confidence display
- Entry / SL / TP1 / TP2
- Daily P/L and position count
- Start/stop bot controls
- VPS API URL and token settings

## Important architecture

The phone is NOT the MT5 execution engine.

Recommended production architecture:

Android APK
    |
 HTTPS + token
    |
VPS API
    |
Python AI service
    |
MetaTrader 5 terminal
    |
Broker

Never put broker credentials inside the Android APK.

## Build

Open this folder in Android Studio.

Use JDK 17 and let Android Studio sync the Gradle project.

Then:

```bash
./gradlew assembleDebug
```

The debug APK will be under:

`app/build/outputs/apk/debug/app-debug.apk`

For Windows:

```bat
gradlew.bat assembleDebug
```

## Backend still required

The UI currently contains safe placeholder behavior for Start/Stop/Refresh so the project can be previewed before a backend exists.

For real operation, implement:

GET  /api/status
POST /api/bot/start
POST /api/bot/stop

The API should return JSON containing the bot status, signal, confidence, prices, daily P/L and position count.

The Python MT5 bot from the companion project should remain on the VPS.
