# LingWing

Version 0.5.0: renamed app branding and approved blue/mint LW wing logo. Existing install, IME and cloud identity identifiers are retained for compatibility.

## 0.4.1 独立登录页与免费不限次数账号

登录界面改为独立页面，Google 与邮箱入口分开，支持密码显隐、注册切换和找回密码。businessstanley8@gmail.com 的免费不限次数权限绑定到服务器上的真实用户 ID，不消耗普通账号每日全站额度；普通账号保留原有限额。仍保留单次长度与并发保护。没有启用支付或扣款。详见 ACCOUNT-LOGIN.md。

## 0.4.0 账号登录与云端额度

在 Keyboard Settings 的 LazyTalk 账号卡片注册或登录，验证邮箱后可直接使用 OpenAI 翻译，不再填写共享访问码。支持邮箱密码及 Google 登录、找回密码、退出和剩余额度查看。Google 提供方已启用，支持邮箱为 businessstanley8@gmail.com，Android 包名及实际 APK 签名与 OAuth 客户端已核对一致。Google 账号选择器仍需真机验收。云端按个人账号限制每日 50 次、单次 500 字符，并设置全站每日 500 次保护。详见 ACCOUNT-LOGIN.md。

## 0.3.2 拼音上排与云端连接准备（历史版本）

组词时上排显示拼音/组合文本，下排显示候选词，两排共同复用原工具栏高度，按键尺寸保持不变。手机设置新增 HTTPS 后端访问码（非 OpenAI 密钥），仅向配置的 HTTPS 服务发送 Bearer 认证；USB HTTP 不发送访问码。访问码存于应用私有 DataStore，应用禁止备份。

云端 WSGI 后端已部署到用户确认的 holysht 项目，认证、限流和输入校验测试通过，实际 HTTPS OpenAI 翻译成功。设置中选择“使用 Google Cloud 连接”，填入独立云端访问码并保存后，可离开电脑和 USB 使用。OpenAI API 密钥不进入 APK，通过 Secret Manager 注入云端；手机仅持有独立访问码。部署说明见 backend/cloud/README.md。

## 0.3.1 候选词复用工具栏

中文、日语、韩语和越南语组词时，候选词直接替换顶部工具栏，不再新增候选行，也不缩小按键。确认或清空组词后恢复原工具栏；翻译输入模式同样复用顶部语言栏。候选词可横向滚动。

## 0.3.0 常用语言与离线输入引擎

当前版本扩展为内置多语言布局方案，语言页支持中文、原文、英文名称和布局编号检索。布局与翻译语言独立；语言切换不访问网络、不下载资源。

- 中文简体：基于 AOSP 55,320 条词库的全拼候选与基础分词，选择候选或空格确认。
- 日语：罗马字转平假名、片假名和 Mozc 开源词数据的逐词候选。为基础逐词输入，不是完整 Mozc 句子转换器。
- 韩语：两套式键位、复合元音、韵尾和音节重组，删除按实际按键回退。
- 越南语：Telex 元音形状与声调输入。其余语言直接字符输入，语言专用字符通过 Shift 或长按候选选择。
- 其他新增布局使用固定版本 FlorisBoard 数据，保留多码点字符按键，键盘几何不随系统 RTL 方向镜像。初次使用词库在后台加载，日语词数据分片缓存，最多保留四份字典。

此版本不代表 Gboard 或 Keyman 的全部语言已完成，也不含这些产品的预测模型和滑行输入。开源来源与授权文本保存在 third_party/ 和 APK assets/licenses/。手机端最终验收仍需实机测试。

## 0.2.16 语言选择可识别性

当前键盘语言采用圆角填色按钮和下拉箭头，显示中文名称。选择页使用中文＋原文双语卡片，例如“泰语 · ไทย”，选中项标注“✓ 当前”。Emoji 入口使用自行绘制的单色线条笑脸图标，不依赖系统表情字体。

## 0.2.15 多语言键盘布局

第一排前十个字符键带 1–0 数字角标，长按仅输入对应数字；底排 ?123 打开数字符号，☺ 打开内置常用 emoji，支持返回当前语言。

普通工具栏使用等宽图标入口：翻译、剪贴板、设置，以及当前键盘语言。点击语言名称选择内置英语、泰语、俄语、乌克兰语、阿拉伯语、希腊语、希伯来语布局，立即改变实际按键并保存选择。此选择独立于翻译目标；不需要下载或联网。Shift 显示另一层字符，泰语采用四行 Kedmanee 布局，横屏按可用高度调整。组合附加符号显示虚线圆以便辨认，实际只输入符号本身。

目前为直接字符输入，不含词典预测、自动纠错或中文拼音／日语／韩语组合引擎。布局数据参考 https://www.unicode.org/cldr/cldr-aux/charts/34/keyboards/layouts/th.html 。

# LazyTalk Keyboard

Latest: **0.2.14 (versionCode 23)** removes the large live-translation text button in favor of a 36dp status/retry icon (6dp idle dot, 16dp progress spinner). Catalog refresh failures no longer appear as a red backend outage in the typing/result area; the language picker keeps its catalog warning/retry. A successful translation clears the stale warning and retries catalog refresh once. Actual translation/insertion errors remain visible. The ordinary toolbar now has Translate and Clipboard entries, no separate square Paste/recognition controls, and English at the far right identifies the currently implemented typing layout independently of the translation target. Google integration/local key was removed; the backend is OpenAI only.


Latest: **0.2.13 (versionCode 22)** adds live translation in Remote/OpenAI mode. Translate opens live mode; editing/importing pauses 500ms before requesting a translation. Stale requests are canceled, the first candidate updates only the IME's tracked insertion range, and no send/editor action is invoked. Existing text before/after the range is preserved. If host text, selection or extraction window changes, automatic insertion pauses with an error; toggle live off/on to restart deliberately. Unsupported editors are left untouched. Language/recognition pickers pause requests without losing the existing translation range. Clearing the source removes only that range. More → 实时翻译 switches to manual Convert mode. Mock does not perform live requests. The same running backend/USB route is still required; this is not offline or standalone cloud hosting. Phone acceptance remains pending.

Latest: **0.2.12 (versionCode 21)** fixes the draft caret inside the non-focusable IME window. Compose's legacy text field suppresses its cursor when LocalWindowInfo.isWindowFocused is false even if the field has local focus. Only the active draft editor receives a delegated WindowInfo with logical focus; real WindowManager flags and host-app focus are unchanged. Platform text-input interception, selection, copy/paste and cursor-based editing remain intact. A native-rendered regression checks actual cursor-color pixels while the surrounding window reports no focus. Phone acceptance is still required.

Latest: **0.2.11 (versionCode 20)** replaces the translation preview with a real editable field: visible cursor, tap/drag selection, long-press floating Copy/Cut/Paste/Select all menu, cursor-based key insertion/backspace and selection replacement. Copy actions are in the standard text-selection toolbar, with no extra Copy/Select all entries in More. Paste my reply also inserts at the cursor/replaces selection. The field no longer contains a Mock prefix. Platform text-input sessions are intercepted so the internal draft does not open another IME. Native selection-toolbar behavior still requires phone acceptance.

Latest: **0.2.10 (versionCode 19)** automatically imports newly copied plain text on opening recognition. A process-memory fingerprint prevents an unchanged clipboard from returning after Clear. Different copied content appends automatically; explicit Paste remains an optional way to re-add unchanged content. No background clipboard listener. Mock remains a sample-only demo; Remote with the running backend provides real translation.

Latest: **0.2.9 (versionCode 18)** also makes the translation draft's left clipboard icon open the recognition list without pasting. This closes the remaining path that reinserted the clipboard after Clear → Done → reopen. Paste remains explicit inside the panel or its named menu item.

Latest: **0.2.8 (versionCode 17)** separates opening recognition from pasting clipboard text. 识别 / More → 识别列表 only opens the list. Clearing then reopening remains empty. Panel 粘贴 and the translation clipboard icon explicitly import the current clip. This fixes a cleared sentence reappearing on reopen.

Latest: **0.2.7 (versionCode 16)** retains the recognition list across Done, hiding the keyboard and ordinary input-session changes. Paste appends/deduplicates up to 50 entries; tap a sentence to switch the target. 清空, entering secure/password fields or process termination erase the temporary list. Each sentence must be copied and explicitly pasted before the next copy; no clipboard monitoring or persistent storage is used. Chinese Pinyin input remains unimplemented. See `COPIED-SENTENCE-IDENTIFICATION.md`.

Removal: **0.2.1** removes the experimental accessibility/chat-language-assist feature at the user's request. The accessibility service, WhatsApp tree reader, auto-reply settings, ML Kit model/dependency and related preference are removed. Manual language selection, offline language catalog, typing, and explicit OpenAI draft translation remain. No accessibility access is required.

Offline languages: **0.1.7** keeps language choices available independently of the backend. The bundled catalog is published before Remote fetch; same-backend verified capabilities are kept in memory, with cached/offline status when unverified. Failed refresh no longer clears the keyboard list. Successful refresh explicitly snapshots the repository flow, fixing lists lost when StateFlow emits no change. A fresh process can choose bundled languages offline; actual AI translation still requires the backend.

Design: **0.1.6** adds keyboard-toolbar Settings and explicit Paste into translation draft, standard half-key QWERTY staggering, larger letter labels, rounded space/action keys and a boxed translation draft. Portrait/landscape bounds and system navigation insets remain covered. Interaction reference: https://support.google.com/gboard/answer/7421372 and https://support.google.com/gboard/answer/7068494 .

Language loading: **0.1.5** distinguishes a failed language-capability load from a search with no results, exposes Retry in keyboard/settings pickers, and retries an empty list when the picker reopens. Iban search and backend-failure recovery are covered by regression tests.

Navigation: **0.1.4** restores system navigation insets around the compact keyboard so three-button navigation cannot cover Space or Enter; landscape side navigation is handled too. 

OpenAI: **0.1.3** adds the OpenAI USB backend. See `backend/README.md`: run `backend/start.ps1` on the computer, then select **Connect OpenAI via USB** in phone Settings. A live English-to-Malay OpenAI request succeeded during development. Credentials stay in the ignored local backend/.env and are excluded from APK/source archives. Standalone use without the computer still needs an authenticated HTTPS deployment.

Compact UI: **0.1.2** uses a compact typing toolbar, an expandable translation draft, reduced landscape key height, side-by-side landscape language search, and landscape results that replace the keys until Edit is selected. Tap Space to type; hold Space to open the system input-method picker. The redundant branding/switch/hide bottom row is removed. Translation is still bounded Mock fixtures unless a real backend is configured.

Previous fix: **0.1.1 (versionCode 2)** installs lifecycle/saved-state/ViewModelStore owners on the IME window decor before Compose attaches. This addresses the device-reported `ViewTreeLifecycleOwner not found` startup crash. See `IME-STARTUP-FIX.md` for the fix and 24-test results, including two Robolectric Android-view regression tests. First test execution downloads an Android test SDK; test caches and lock files stay inside the build directory.

Packaging correction (2026-10-18): the original APK was rejected by Android with error -124 because the Windows sandbox adapter wrote `resources.arsc` as DEFLATE. The adapter now writes real STORED ZIP entries, `androidResources.noCompress` explicitly includes `arsc`, and every `assembleDebug` runs `verifyDebugApkPackaging` to reject compressed or non-4-byte-aligned resource tables. Signature verification and ordinary zipalign checks alone do not detect this error, because zipalign permits compressed entries. Use the rebuilt APK, not an earlier WhatsApp attachment.

Native Android IME (Kotlin, minSdk 26). A Latin keyboard with an explicit AI translation draft, dynamic language capabilities, and 1–3 selectable complete-sentence candidates. This is an installable system input method, not an in-app keyboard simulation.

## Architecture

V1 uses one Gradle `:app` module with package-by-feature boundaries. This keeps Android/Compose integration practical without empty modules. Repository interfaces, domain models and use cases can move into separate Gradle modules later.

```text
Android
├─ MainActivity → Home / SettingsScreen → SettingsViewModel
└─ LazyTalkInputMethodService (system lifecycle, EditorInfo, window)
   ├─ InputConnectionController → host app InputConnection
   └─ KeyboardView → KeyboardAction → KeyboardViewModel / StateFlow
      └─ TranslateTextUseCase → TranslationRepository interface
         └─ TranslationRepositoryImpl → TranslationService
            ├─ MockTranslationService (bounded offline fixtures)
            └─ RemoteTranslationService → ApiClient → your Backend → AI provider
```

Dependencies are supplied by `app/di/AppContainer`. An explicit container was chosen over Hilt to keep ownership of the unusual IME View lifecycle visible and testable. Application-scoped repositories and one lazy HTTP client are shared; the keyboard ViewModel is owned by the IME's ViewModelStore and cleared at Service destruction. `ImeViewOwner` supplies lifecycle and saved-state owners required by Compose. Transient draft contents never go into SavedStateHandle or DataStore.

### Packages and main files

All Kotlin packages are under `app/src/main/java/com/lazytalk/keyboard/`:

| Location | Responsibility |
| --- | --- |
| `app/di/AppContainer.kt` | Dependency construction and centralized debug/release translation configuration |
| `core/model/Models.kt` | Domain Language, Tone, request/result and settings models |
| `core/common/AppError.kt` | Safe user-facing error model; no raw server exception messages |
| `core/data/Repositories.kt` | Translation, language and settings interfaces |
| `core/data/settings/` | Atomic DataStore edits; settings codec and safe defaults |
| `core/data/language/` | Registry/Backend capabilities, search, favorites and recent choices |
| `core/data/translation/` | Provider selection and exception boundary |
| `core/network/` | HTTPS policy, timeouts, cancellable HTTP, DTOs and domain mappers |
| `feature/keyboard/ime/` | Real InputMethodService, security policy, InputConnection and lifecycle owners |
| `feature/keyboard/domain/` | Translate validation and source/target swap logic |
| `feature/keyboard/presentation/` | Single keyboard state, actions, effects and request invalidation |
| `feature/keyboard/components/` | Compose keyboard, keys, candidate buttons and controls |
| `feature/language/presentation/` | Search, recent/favorite/popular/all language picker |
| `feature/settings/presentation/` | Settings UI and ViewModel |
| `translation/mock/`, `translation/remote/` | Interchangeable translation data sources |
| `ui/` | Light/dark theme |

`AndroidManifest.xml` registers the exported IME service protected by `android.permission.BIND_INPUT_METHOD`, the `android.view.InputMethod` intent and `res/xml/input_method.xml`. The system settings entry uses a settings Activity alias. Ordinary key events and selected candidates travel as effects to `InputConnectionController`; only this Android bridge invokes `commitText`, deletion or editor actions.

## Translation flow

Normal keys type directly into the host app. Tap **AI draft** to type into an in-memory translation draft instead. **Selection** explicitly reads the host's selected text; **Paste** explicitly reads clipboard text. Neither action sends text over the network. Select source (default Auto Detect), target and tone, then tap **Convert**. Tap a candidate to commit it to the active host editor. When a host selection remains selected, `commitText` replaces that selection. Otherwise the sentence is inserted at the cursor.

Requests are invalidated on draft/language/tone/configuration changes and on input-session changes. In-flight HTTP calls are cancelled with their coroutine. A generation counter prevents stale results from updating a later session. Errors appear in the keyboard and **Retry / Regenerate** resends only following an explicit tap.

Shift cycles **off → one capital → CAPS → off**. Backspace repeats after 400 ms; Unicode deletion uses code points. Enter maps Done/Next/Previous/Search/Send/Go using EditorInfo and respects `IME_FLAG_NO_ENTER_ACTION`. Fullscreen extraction is disabled. Header/candidates scroll within available space; the key rows stay below them. Language search inside the IME uses its own local keys so a second system keyboard is not needed.

## Language architecture

`Language` is a data class, never a fixed enum. Debug Mock mode loads `app/src/main/assets/languages.json`; Remote mode loads `GET /languages`. The current registry contains 30 language entries, with status badges. Registry presence is not a claim of working Mock translation for that language. Mock phrases cover en/zh/ms and one Iban fixture only; other entries exercise a scalable picker. Remote capabilities are replaced by the Backend response.

Language codes are opaque provider identifiers (including `iban` as requested). A Backend may return `iba`, a BCP-47 tag or a new language without changing keyboard logic. Both API endpoints must agree on identifiers. Source `auto` is a special selection, not a target language. Swap resolves `auto` from the last detected result; before detection it explains that a conversion is required instead of inventing a source. Favorites/recent are bounded preferences, not a database. Adding registry entries does not add an input-script engine.

## Privacy model

- No keystroke logging, analytics, transcript history, accounts, database or input backup.
- Ordinary typing commits only to the host editor. Only explicit Convert sends the draft in Remote mode. Language capability requests contain no draft.
- `SecureFieldDetector` blocks passwords (including visible/web passwords), all number/phone fields, `TYPE_NULL` editors and `IME_FLAG_NO_PERSONALIZED_LEARNING` editors. This conservative numeric policy covers common PIN/payment fields. Keyboard typing continues to work.
- Drafts/candidates are erased and requests cancelled when the keyboard hides or an input session ends/changes. Sensitive field entry erases prior drafts; importing clipboard/selected text is blocked there.
- No input content, API bodies, tokens or server exception details are logged. OkHttp has no logging interceptor. Android backup is disabled.
- HTTPS only, no URL credentials/query/fragment, no redirect following, no AI provider key in the APK. Timeout: connect 10 s, read 25 s, total 35 s. Responses are bounded to 256 KiB.
- Android provides no universal credit-card flag. Apps can mislabel sensitive content as ordinary text; the IME cannot reliably infer the meaning of every plain text field. Users should never explicitly convert secrets.
- Cancelling a request prevents stale UI updates; it cannot undo data that has already reached a Backend. Your Backend must define retention and authentication before production.

## Mock mode

Debug defaults to **MOCK** and needs no network. Supported fixture inputs include:

| Input | Example target |
| --- | --- |
| 我等下才过去，你先吃。 | English / Malay |
| I will call you when I arrive. | Malay / Chinese |
| Where are you now? | Chinese / Malay |
| Awak dekat mana sekarang? / kat mana sekarang | Chinese / English |
| 等下我才过去 | English |
| 你吃了吗？ | Malay / English |
| Nama berita? | English / Malay (Iban fixture, beta) |

Fixture equivalents can also be used as source text. Unknown phrases/language pairs show a clear unsupported Mock error. Mock does **not** apply tone rewrites; the UI explicitly says this. Remote transmits all four tone values. Iban fixture quality is not linguistically validated.

## Remote API contract

Set an HTTPS Backend base URL and choose REMOTE in the debug Settings page. A base URL `https://host/api/` produces `/api/languages` and `/api/translate`. The Backend must be reachable from the phone and use a trusted certificate. Cleartext localhost APIs are intentionally disallowed.

`GET /languages`:

```json
{"languages":[{"code":"en","name":"English","nativeName":"English","status":"stable"},{"code":"ms","name":"Malay","nativeName":"Bahasa Melayu","status":"stable"},{"code":"iban","name":"Iban","nativeName":"Iban","status":"beta"}]}
```

`POST /translate`, `Content-Type: application/json`:

```json
{"text":"where are you now","sourceLanguage":"auto","targetLanguage":"zh","tone":"natural"}
```

```json
{"detectedLanguage":"en","candidates":["你现在在哪里？","你现在在哪儿？","你现在在哪？"]}
```

Response DTOs are validated/mapped to domain models before reaching UI. Missing detection or empty candidates fail safely. Whitespace/duplicate candidates are removed; the result is capped at three. HTTP 422 maps to unsupported language; other non-success statuses map to a safe server error. Tone values: `natural`, `casual`, `polite`, `short`.

The Backend, not this client, must own the provider prompt, credentials and provider switching. The prompt should preserve meaning, people, numbers, URLs, proper nouns and emoji intent; produce natural target-language grammar; avoid inventing or dropping facts; and return 1–3 sentences in the requested tone. Source text must be treated as untrusted text, never as instructions to the model. A production Backend is **not included** in V1.

Release builds force Remote mode and ignore persisted development URLs. Set the non-secret `BACKEND_URL` field in the release build configuration before production. It is deliberately empty today, so release AI fails closed with a configuration error. Release settings hide Mock and development URL controls. No release signing key is supplied.

## Build instructions

Requirements: Android Studio, JDK 17 or compatible bundled JBR (validated with JBR 21), Android SDK platform 36, internet for Gradle dependencies. AGP 9.1.1 provides built-in Kotlin; Compose compiler plugin 2.2.10 is configured separately. Gradle Wrapper 9.3.1 is included.

1. Open this directory in Android Studio and let Gradle sync.
2. Configure `local.properties` with your local SDK directory (not version controlled).
3. In PowerShell, set `$env:JAVA_HOME` to your JDK/JBR directory if Java is not on PATH.
4. Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

Standard APK path: `app/build/outputs/apk/debug/app-debug.apk`. Install with Android Studio or `adb install -r app/build/outputs/apk/debug/app-debug.apk`. The debug APK uses a local development signing certificate and must not be distributed as a production release.

An optional `-Plazytalk.buildRoot=C:/short/path/app` changes the app build directory for Windows environments with long-path restrictions. Kotlin compilation runs in-process with incremental compilation disabled to avoid sandbox temporary-file failures; these two Gradle properties can be revisited outside this environment.

In this Windows execution sandbox, the official native AAPT2 ZIP writer also crashes on a minimal empty App. `tools/build-windows-sandbox.ps1` is an **optional environment workaround**, not a replacement Android compiler. It compiles a small C# daemon adapter, invokes the official SDK AAPT2 with `--output-to-dir`, packages those unchanged resources with .NET ZIP, then lets Gradle compile, dex, sign and package the APK normally. It does not skip resources or checks. Run `powershell -ExecutionPolicy Bypass -File tools/build-windows-sandbox.ps1` only if the ordinary Gradle build reproduces that native crash. The standard build does not require C# or this adapter.

## Testing

`app/src/test` contains tests for repositories/provider selection, UseCase privacy guards, language registry/search/favorites/recents, JSON serialization/response mapping, automatic detection, swap, secure-field detection, Mock fixtures, settings decoding, actual DataStore file reopening and keyboard state/cancellation/commit effects. JVM Android stubs are used only for constants; these tests do not prove Android framework integration or real WhatsApp compatibility.

See `BUILD-VERIFICATION.md` for the actual checks performed in this environment. Physical-device installation and cross-app acceptance must be verified separately when a phone is available.

### Phone / WhatsApp acceptance

1. Install the debug APK (allow installation from the source used if Android asks).
2. Open LazyTalk → **Enable Keyboard** → enable LazyTalk in Android settings.
3. Return → **Select Keyboard** → choose **LazyTalk Keyboard**.
4. Open WhatsApp → focus a chat field. The real IME should appear.
5. For Chinese, first type `我等下才过去，你先吃。` with an existing Chinese keyboard and select it, then switch to LazyTalk and tap **Selection**. Alternatively copy the phrase and use **Paste**. V1 does not have a Pinyin engine.
6. Keep source Auto Detect, target English → **Convert** → tap `I'll come over later. You eat first.`. Check that the complete sentence enters the WhatsApp editor; it is not automatically sent.
7. Choose target **Bahasa Melayu**. Tap **AI draft**, type `I will call you when I arrive.` → Convert → detected English → tap a Malay candidate.
8. To test swapping, convert first and tap ⇄ while the detected source is still present (candidate insertion clears detection). Confirm `Malay → English`.
9. Test Shift/Caps, long-press delete, selection replacement, Send/Next/Search, hide/reopen, rotation, dark mode and a password/numeric field. Translation must be disabled in secure fields and typing must still function.

## Known limitations and roadmap

- No real AI Backend or provider keys included. Remote contract is implemented, but live AI quality depends on the server you deploy.
- Latin keyboard only; no Pinyin, Indic/Arabic/Japanese input engine, autocorrect, prediction, gesture typing, voice or emoji picker. You can import Unicode text explicitly.
- Draft edits append/delete at the end; no full draft cursor editor in V1. Numeric fields are conservative secure mode, including benign numeric inputs.
- Local registry is a picker seed, not a claim that every language is supported. Mock tone controls send the correct future contract but fixture outputs are unchanged.
- Compact header scrolls in landscape; tablet/large-font ergonomics and TalkBack need device testing.
- No auth/rate limiting/subscription/production signing. Release Remote stays disabled until a Backend URL is configured.
- Next steps: deploy authenticated `/languages` + `/translate`, add per-user quotas and server-side provider selection, validate with native speakers (especially Iban), run real-device IME/privacy/accessibility tests, then add script engines/voice with separate feature boundaries.

## References

IME registration and lifecycle follow the official Android [Create an input method](https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method) and [InputMethodService API](https://developer.android.com/reference/android/inputmethodservice/InputMethodService).









