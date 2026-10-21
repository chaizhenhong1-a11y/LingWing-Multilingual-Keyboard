# LingWing Keyboard

Native Android multilingual keyboard with offline typing engines and account-based OpenAI translation.

**Latest APK:** `LingWing-debug.apk` — **0.5.5 (40)**.

## Project structure

- `app/`: Android source, dictionaries, resources and tests.
- `backend/`: local development server and Cloud Run backend.
- `gradle/`, `gradlew`, `gradlew.bat`: Gradle wrapper.
- `third_party/`: licenses and attribution.
- `tools/`, `.sandbox-tools/`: build helpers; these are required and were retained.
- `docs/`: release notes, verification records and historical documentation.
- `使用说明.md`: current Chinese usage instructions.

## Build

Open the project in Android Studio, or run `./gradlew.bat :app:assembleDebug` from this directory. Generated APKs appear in `app/build/outputs/apk/debug/`.
The Codex sandbox build uses the retained AAPT adapter. Project caches and generated build outputs will be recreated when needed.

## Configuration

`backend/.env`, `local.properties` and `gradle.properties` were retained and verified unchanged during cleanup. Do not publish the OpenAI server key.
Package/IME identifiers and the cloud service address remain stable for upgrade compatibility. The Gradle project name is `LingWingKeyboard`.

## Verification

Version 0.5.3 passed 123 Android tests, assembleDebug, lintDebug, APK alignment and signing checks. It was installed successfully on the connected HONOR X9c.
The prior vibration issue was traced to system touch feedback being OFF; version 0.5.3 uses a short motor pulse controlled by the app setting. Motor perception should be confirmed by trying the keyboard on the phone.
