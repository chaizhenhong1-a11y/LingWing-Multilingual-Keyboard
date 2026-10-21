# Project cleanup

Removed obsolete APK copies and regenerable project caches. Retained one latest root APK, source code, dictionaries, build helpers and all credentials/configuration.

Freed approximately 490.2 MB. Configuration SHA256 checks were unchanged. Historical documents were archived rather than deleted.

Removed:
- LazyTalkKeyboard-0.4.1.apk
- LazyTalkKeyboard-0.4.2.apk
- LazyTalkKeyboard-0.4.3.apk
- LazyTalkKeyboard-0.4.4.apk
- LazyTalkKeyboard-0.4.5.apk
- LazyTalkKeyboard-0.5.0.apk
- LazyTalkKeyboard-0.5.1.apk
- LazyTalkKeyboard-0.5.2.apk
- LazyTalkKeyboard-0.5.3.apk
- LazyTalkKeyboard-debug.apk
- LingWing-0.5.0.apk
- LingWing-0.5.1.apk
- LingWing-0.5.2.apk
- LingWing-0.5.3.apk
- app\build
- .gradle
- .kotlin
- backend\__pycache__

Project directory renamed successfully to `C:\project development\LingWingKeyboard`. The old directory no longer exists. ADB server PID 20196 held the directory; it was stopped before renaming. Gradle rootProject.name and the current sandbox build script were updated. Latest root APK is LingWing-debug.apk (0.5.3 / 38).
