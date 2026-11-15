# TH Launcher

**An independently developed Minecraft: Java Edition launcher for Android.**

TH Launcher is its own product and codebase. The product owner designs the app UI; this repository focuses on the Android foundation and launcher systems.

## Current implementation

- Native Android/Kotlin project scaffold
- GitHub Actions debug-APK workflow
- Domain model for game profiles and Minecraft versions
- Private launcher directory layout
- Public Minecraft version-manifest reader (network work must run off the UI thread)
- Java runtime descriptor and validation model
- Validated game-launch request model

## Not yet a playable launcher

The current code does **not** yet install Minecraft, download libraries/assets, install or bundle an Android-compatible Java runtime, authenticate Microsoft/Minecraft accounts, or start/render the game. These require separate implementations and device testing. Do not treat the current debug APK as a playable launcher.

## Build a debug APK

1. Open the **Actions** tab.
2. Select **Android APK**.
3. Run the workflow with **Run workflow**, or push a commit to `main`.
4. Open the completed run and download the `TH-Launcher-debug` artifact.

## Technical baseline

- Native Android application
- Kotlin
- Android Gradle Plugin 8.7.3
- JDK 17
- Minimum Android API 26
- Application ID: `com.thlauncher.app`

## Development principles

- Keep the product UI owner-designed; do not add a third-party launcher UI.
- Keep launcher logic modular and testable.
- Never collect or store Microsoft passwords.
- Use the supported Microsoft/Minecraft authorization flow for account access.
- Use official public Minecraft metadata and respect all game/runtime licenses and redistribution terms.
- Never claim runtime or game-launch support until it is implemented and tested on Android.

## Planned next modules

1. Version-detail metadata parsing and integrity-checked download engine
2. Minecraft version installer (client, libraries, assets, natives)
3. Android-compatible Java runtime integration and architecture detection
4. Microsoft/Minecraft account authorization and token lifecycle
5. Game argument generation and Android process/rendering integration
6. Profile persistence, diagnostics, and crash logs

## License

No open-source license has been assigned yet. All rights remain with the repository owner unless a license is added.
