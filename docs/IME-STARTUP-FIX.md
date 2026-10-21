# IME window startup fix — version 0.1.1 (code 2)

The phone's crash log identified `IllegalStateException: ViewTreeLifecycleOwner not found` on the IME window's `parentPanel`, while Compose was creating its window recomposer. This caused the keyboard process to crash when selected.

The previous implementation set lifecycle, saved-state and ViewModelStore owners only on the ComposeView. Compose looks up its window recomposer from an ancestor, so those child-view owners could not satisfy the window-root lookup.

## Fix

- `ImeViewOwner.installOn(View)` installs all three owners consistently.
- `LazyTalkInputMethodService.onCreateInputView()` installs those owners on `window.window.decorView` **before** constructing/returning the ComposeView.
- The ComposeView retains the same owner, so the window's native ancestors and Compose content share a single lifecycle.
- Version changed to `0.1.1`, versionCode `2`.
- The previous resource-table packaging fix and automatic packaging guard remain enabled.

## Executed checks

- **24 JVM tests passed, 0 failures, 0 skipped.**
- Two tests run with Robolectric's Android API 28 view environment:
  - `unownedWindowReproducesTheReportedComposeCrash` reproduces the exception using Compose's real window recomposer API on an unowned native view.
  - `keyboardWindowAncestorsHaveOwnersBeforeComposeAttaches` creates the actual IME Service and verifies that its keyboard view and window decor expose the same lifecycle/saved-state/ViewModelStore owners before attachment.
- Kotlin compilation, lint (**0 errors, 14 warnings**) and `assembleDebug` passed.
- The final APK packaging guard passed: `resources.arsc` is STORED, at offset **10,759,168**, divisible by 4.
- APK signature verification passed (v2).

The first attempt to start Robolectric was blocked because its download lock defaulted to the user's home directory. Test JVM home, temporary files and SDK download cache now reside in the build directory. After that change the actual Android-view tests executed and passed; they were not skipped.

## Delivered APK

```text
C:\project development\LazyTalkKeyboard\LazyTalkKeyboard-debug.apk
```

SHA-256: `1718ED85FE5BE0C5F03B54F75ABE029E958C82D3FD5788941DFA5B01F5EDEB5C`

Install from the computer's own PowerShell:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "C:\project development\LazyTalkKeyboard\LazyTalkKeyboard-debug.apk"
```

After installation, focus a chat field and select LazyTalk again. The original APK's installation succeeded on the user's phone; **the new version's live keyboard display on HONOR still needs device confirmation**. These Android-view JVM tests do not replace real-device IME acceptance.
