# WePlay

Party games and voice rooms, built with Kotlin and Jetpack Compose.

## Features

- Sign in with Google, phone or email, then set name, birthday and profile photo
- Coin top-up packs with a local demo checkout, no real payments
- Five-tab app: games, voice rooms, discover, messages, profile
- Ludo against three local bots
- Who's the Spy, pass and play for 3 to 8 players
- Placeholder tiles for upcoming games

## Build

Open the project in Android Studio, or run:

```bash
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

The app runs fully offline. There is no backend and no network access.
