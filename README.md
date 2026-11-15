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
- Customizable control-profile model with default keyboard and mouse bindings

## Modrinth modpack installation

- The Modrinth .mrpack installer reads the pack manifest, copies the declared overrides, downloads manifest files, and verifies each file's SHA-1.
- Installation is isolated to a new/empty instance directory and reports per-file progress.
- The installer reports required Minecraft version and loader identifiers; it does not yet install the matching Fabric/Forge/NeoForge loader or provision the required Java runtime. A pack must not be advertised as launch-ready until those steps are completed.
- CurseForge packs are not claimed as fully supported by this Modrinth installer.

## Download engine

- HTTPS streaming downloads with Range-based resume when supported by the server.
- Per-download byte counts, optional total size, transfer speed, cancellation checks, and SHA-1/SHA-256 verification.
- Download operations must run on a worker thread; the UI should observe progress callbacks and never block the main thread.
- This engine is a reusable primitive. Minecraft version installation still needs to orchestrate version JSON, client JAR, libraries, assets, native extraction, Java runtime provisioning, and launch arguments.

## Real feature foundations added

- Android process/device memory snapshot and frame-callback FPS counter.
- Persistent local offline player profiles. Offline profiles do not authenticate with Microsoft or grant online-mode access.
- ZIP modpack import into an isolated instance directory, with path traversal checks, cancellation, progress callbacks, and expansion/file-count limits.
- Modpack import currently extracts archive contents and identifies common manifest formats. It does not yet resolve/download remote mod dependencies, install loaders, or guarantee a pack is launchable.

## Performance and modpack requirements

- Keep installation and archive extraction off the main/UI thread.
- Display measured app memory, available device memory, and FPS only when the corresponding game/render surface is active.
- Import Modrinth/CurseForge packs through user-selected files and verify manifest file hashes and download URLs in the full installer.
- Preserve the user's selected instance and never overwrite an existing instance during import.

## First-run storage and download experience

- The first-run setup should explain where game files will be stored before starting installation.
- Prefer app-private storage by default; Android does not require broad file access permission for the app to manage its own private files.
- If the user chooses a shared folder, request access through Android's system folder picker (Storage Access Framework) rather than requesting unrestricted storage access.
- The owner-designed UI should expose a separate download panel with per-component progress for runtime, game version, libraries, and assets, including progress, size, speed, and retry/cancel states.

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
- Keep keyboard, mouse, touch, and future gamepad mappings configurable per account and Android device.
- Never claim runtime or game-launch support until it is implemented and tested on Android.

## Planned next modules

1. Version-detail metadata parsing and integrity-checked download engine
2. Minecraft version installer (client, libraries, assets, natives)
3. Android-compatible Java runtime integration and architecture detection
4. Microsoft/Minecraft account authorization and token lifecycle
5. Game argument generation and Android process/rendering integration
6. Profile persistence, diagnostics, and crash logs
7. Android keyboard/mouse/touch event routing and in-game control customization
8. AI diagnostics and owner-designed AI UI customization, with isolated per-account and per-device profiles

## License

No open-source license has been assigned yet. All rights remain with the repository owner unless a license is added.
