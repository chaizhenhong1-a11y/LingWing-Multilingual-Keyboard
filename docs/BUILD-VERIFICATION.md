# Build verification — 2026-10-18 (Asia/Kuala_Lumpur)

## Correction: original APK installation failure

The original signature-verified APK failed actual phone installation with Android error **-124**: `resources.arsc` was compressed (ZIP method 8), at offset 10,759,257 (not 4-byte aligned). The earlier checks were insufficient; successful Gradle compilation, unit tests, signature verification and zipalign did not establish installability.

Root cause: the optional Windows adapter used .NET Framework `CompressionLevel.NoCompression`, which still emits DEFLATE entries instead of ZIP method 0. It now writes actual STORED entries. The Gradle configuration also explicitly excludes `arsc` from compression, and a new `verifyDebugApkPackaging` task checks the final APK's central/local ZIP headers and resource data alignment on every `assembleDebug`.

The historical build details below describe the original package. Replacement APK verification results are recorded separately in `INSTALL-FIX-VERIFICATION.md`. Device runtime acceptance remains pending until the replacement is installed.

## Actual results

| Check | Result |
| --- | --- |
| Gradle project configuration and dependency resolution | Passed |
| `:app:compileDebugKotlin` | Passed |
| `:app:testDebugUnitTest` | **22 tests, 0 failures, 0 errors, 0 skipped** |
| Actual DataStore file persistence/reopening | Passed (included in the 22 tests) |
| `:app:lintDebug` | **0 errors, 14 warnings** |
| `:app:assembleDebug` | **Passed — BUILD SUCCESSFUL** |
| APK signature verification (`apksigner verify --verbose`) | Passed, APK Signature Scheme v2, one signer |
| Packaged manifest inspection | IME Service, BIND_INPUT_METHOD, InputMethod intent/metadata present; minSdk 26, targetSdk 36 |
| Android Studio GUI sync | Not run; command-line Gradle configuration/resolution/compilation were performed |
| Device installation / live IME / WhatsApp acceptance | **Not verified in this environment** |
| Real AI Backend integration | **Not verified; no live Backend supplied** |

Final successful build ran 53 tasks (29 executed, 24 up-to-date) in 2m 6s. The 14 lint warnings concern newer dependency/Gradle/target versions and the deliberate screen-height-based IME size limit. No lint baseline or error suppression was used to make checks pass.

## APK

- Standard generated location: `app/build/outputs/apk/debug/app-debug.apk`
- Delivery copy beside this project folder: `../LazyTalkKeyboard-debug.apk`
- Size: **11,278,044 bytes**
- SHA-256: `5E47BC9155A3D0FE490E9AECFEEC6EFD77C133E5F31A056E2CF21E46DAB0EB62`
- Application ID: `com.lazytalk.keyboard`
- Version: `0.1.0` (`versionCode=1`)
- Development/debug signing only.

## Environment problems reproduced and resolved

1. Kotlin incremental compilation failed moving backups into the Windows execution sandbox's virtual temporary directory. Compilation now runs in-process with incremental compilation disabled.
2. Official native AAPT2 repeatedly exited with `0xC0000005`. This reproduced with an independent minimal empty manifest and multiple installed SDK Build Tools versions. The resulting ZIP lacked its central directory. The same official linker succeeded using `--output-to-dir`.
3. The optional `tools/AaptDirectoryAdapter.cs` + `tools/build-windows-sandbox.ps1` invokes official SDK AAPT2 to compile/link, pre-creates required resource subdirectories, then packages the unchanged directory output using .NET ZIP. Gradle subsequently dexes, signs and packages the APK. Resource processing, tests and lint were all executed, not skipped.
4. The actual DataStore test initially failed renaming a file inside the sandbox's virtual temp directory. Test JVM temp files now reside under `app/build/test-tmp`; the persistence test then passed.
5. Lint caught a restricted ViewModelStore API and a Windows SDK property escaping error. Both were fixed. The IME now obtains its ViewModel through the public ViewModelProvider API.

Successful command from the workspace root, with JBR on JAVA_HOME:

```powershell
& outputs\LazyTalkKeyboard\tools\build-windows-sandbox.ps1 -BuildTemp (Resolve-Path work\tmp).Path
```

Normal environments should first use the standard Gradle commands in README. The optional workaround is documented so the successful build can be reproduced here.

## Device-test limitation

ADB became available but reported no connected phone. A fresh disposable local emulator was attempted. Hardware acceleration failed with `Unable to open AEHD device: ERROR_ACCESS_DENIED`; a software-acceleration attempt also did not produce a running device. No physical device, emulator UI, dark-mode/rotation/TalkBack session or real WhatsApp field was available for acceptance testing.

Consequently, the APK is built and signature-verified, and the IME manifest is inspected, but installation and runtime compatibility are **not claimed as tested**. Follow `使用说明.md` / README on an Android phone to perform the final cross-app acceptance flow. The default build uses bounded offline Mock fixtures; a real AI service remains a separate integration step.
