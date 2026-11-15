# TH Launcher

**An independently developed Minecraft: Java Edition launcher for Android.**

TH Launcher is its own product and codebase. The app identity, visual design, architecture, and launcher features are developed for this project. The application UI is intentionally not templated: the product owner will design the screens.

## Project status

This repository currently contains the initial native Android/Kotlin project scaffold and a GitHub Actions debug-APK build workflow.

**It is not yet a working Minecraft launcher.** Minecraft installation, Java runtime integration, game launching, account authentication, version management, and mod-loader support have not been implemented.

## Build a debug APK

1. Open the **Actions** tab.
2. Select **Android APK**.
3. Run the workflow with **Run workflow**, or push a commit to `main`.
4. Open the completed run and download the `TH-Launcher-debug` artifact.

The APK is currently a minimal Android shell, not a playable launcher.

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
- Do not collect or store Microsoft passwords.
- Implement Microsoft/Minecraft sign-in through the supported authorization flow when account support is added.
- Integrate Java runtime and game components only with licenses and redistribution terms respected.

## Planned modules

- Runtime manager
- Minecraft version metadata and installer
- Game profile and launch configuration
- Account authorization
- Mod-loader and mod-profile management
- Download queue and progress reporting
- Diagnostics and crash reports

## License

No open-source license has been assigned yet. All rights remain with the repository owner unless a license is added.
