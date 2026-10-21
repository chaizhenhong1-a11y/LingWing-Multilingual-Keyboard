# APK installation packaging fix — 2026-10-18

The user reported Android install error **-124**. This was an APK packaging defect, not a USB, driver or phone-settings problem.

## Root cause and correction

The original Windows resource adapter used .NET Framework `CompressionLevel.NoCompression`, which still emitted DEFLATE (ZIP method 8). Android requires the resource table to be stored with ZIP method 0 and aligned on a 4-byte boundary for apps targeting API 30+.

- The adapter now emits real STORED ZIP entries, with valid CRCs and 4-byte alignment.
- Gradle explicitly sets `androidResources.noCompress += "arsc"`.
- Every `assembleDebug` now runs `verifyDebugApkPackaging`, inspecting the final APK's ZIP central/local headers for the resource compression method and exact data offset. A bad package fails the build.
- No keyboard functionality, target SDK or security controls were removed to work around the error.

## Actual replacement APK results

| Check | Result |
| --- | --- |
| Gradle build | BUILD SUCCESSFUL |
| Unit tests | 22 passed, 0 failures |
| Lint | Passed |
| Final resource table compression | **STORED, ZIP method 0** |
| Resource table data offset | **10,759,168 bytes**, divisible by 4 |
| Automatic Gradle packaging check | Passed |
| Independent Python ZIP/header check | Passed |
| CRC verification of all ZIP entries | Passed |
| `zipalign -c -P 16 -v 4` | Passed; `resources.arsc (OK)` |
| APK signature verification | Passed, v2 signature |
| Decompressed resource table vs original | Byte-for-byte identical |
| ZIP entry names vs original | Identical |

Replacement size: **11,277,888 bytes**.

SHA-256: `E20E2419F0E829F2A9EE06B342577DAA4B680778F60C4026D633F3E8E122427F`.

The replacement has been copied to:

```text
C:\project development\LazyTalkKeyboard\LazyTalkKeyboard-debug.apk
```

The normal generated artifact is `app/build/outputs/apk/debug/app-debug.apk`.

## Install on the connected phone

Run in the computer's own PowerShell (which can access the authorized device):

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "C:\project development\LazyTalkKeyboard\LazyTalkKeyboard-debug.apk"
```

The reported -124 condition has been directly checked and corrected. Successful phone installation and IME runtime acceptance still require the device command result; this report does not claim those have already been performed.

Official Android requirement: [Compressed resource files](https://developer.android.com/about/versions/11/behavior-changes-11#app-packaging).
