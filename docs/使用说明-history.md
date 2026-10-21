# LazyTalk Keyboard V1

这是原生 Android 系统输入法项目，包含 Compose 首页/设置/键盘、InputMethodService、离线 Mock 翻译和 Remote API 接口。最低 Android 8.0（API 26）。

## 安装与启用

1. 把交付的 `LazyTalkKeyboard-debug.apk` 传到 Android 手机，点击安装；按系统提示允许该来源安装应用。
2. 打开 **LazyTalk Keyboard** → **Enable Keyboard** → 在 Android 设置中启用。
3. 返回 App → **Select Keyboard** → 选择 **LazyTalk Keyboard**。
4. 打开 WhatsApp、浏览器或 App 首页的测试输入框，点击文字输入位置。

Android 对所有第三方系统输入法都会显示输入安全提示。LazyTalk 的普通按键只发送给当前输入框，Remote 模式下只有主动点击 Convert 才会发送翻译草稿。

## 日常输入与翻译

- 普通键盘模式：按键直接进入当前 App。
- 点击 **AI draft**：按键进入待翻译草稿，不立即写入聊天框。
- **Selection**：明确导入当前 App 中已经选中的文字。
- **Paste**：明确读取剪贴板中的文本到草稿。
- 选择目标语言及 Natural / Casual / Polite / Short，然后点击 **Convert**。
- 点击候选完整句子：通过 `InputConnection.commitText()` 插入当前 App；有选中文字时会替换选中文字。不会自动发送聊天消息。
- **Retry / Regenerate**：重新提交当前草稿。
- **⇄**：交换语言；Auto Detect 需要先完成一次转换，以取得已检测语言。
- Shift 依次切换：关闭 → 单次大写 → CAPS → 关闭。长按删除键会连续删除。
- **123 / ABC / #+=** 切换数字、字母和符号。**Hide** 隐藏键盘，**🌐** 切换系统输入法。

## 两个验收例子

默认是离线 **Mock**。它只覆盖明确的测试短句，不是真实全球 AI。

### 中文 → 英文

V1 没有中文拼音引擎。先用已有中文输入法输入并选中：

> 我等下才过去，你先吃。

切换到 LazyTalk → **Selection**；也可先复制该句再点击 **Paste**。

Source 保持 **Auto Detect**，Target 选择 **English** → Convert。

候选包括：

> I'll come over later. You eat first.

点击候选，检查它是否完整进入 WhatsApp 输入框。

### 英文 → 马来文

选择 Target：**Bahasa Melayu** → AI draft → 输入：

> I will call you when I arrive.

Convert → 检测 English，候选例如：

> Saya akan telefon awak apabila saya sampai.

可在点击候选前测试 ⇄，确认变成 Malay → English。插入候选后草稿及检测结果会清空。

## 真实完成的代码

- 真正的 Android IME 注册、Service 生命周期与 Compose 生命周期桥接。
- 字母、大小写、数字、符号、空格、删除、换行及 EditorInfo action。
- 草稿 → 转换 → 最多三个候选 → 当前输入框插入的完整实现。
- StateFlow 状态、Action/Effect、请求取消及防止旧输入框事件进入新输入框。
- 动态 Language model、本地 JSON registry、Remote `/languages`、搜索、收藏和最近语言。
- DataStore 设置，统一依赖容器，UseCase / Repository / DTO / Mapper 边界。
- 密码及敏感输入禁用翻译、HTTPS、超时、错误处理；不保存输入历史，不打印输入内容。

以上是实现范围；实际构建和运行验证范围请查看 `BUILD-VERIFICATION.md`。

## Mock 与尚未完成的部分

Mock 是有限样例集，支持 README 所列句子及对应表达。其他句子会显示不支持提示。Mock 不改写语气，键盘会明确显示这一点。Iban 只有一个 beta 测试样例，尚未经母语人士验证。

真实 AI Backend 没有部署；自然表达质量、四种语气的实际改写和任意语言翻译，需要接入自己的服务。客户端已实现 `GET /languages`、`POST /translate`，代码入口是 `translation/remote/RemoteTranslationService.kt` 和 `core/network/ApiClient.kt`。

Debug 设置允许 Mock / Remote 和 HTTPS Backend URL。Release 会忽略开发 URL、强制 Remote；必须在发布配置中填写非机密 Backend URL，否则转换会安全地显示配置错误。没有任何第三方 AI Secret 写进 APK。

数字、电话输入框也保守视为敏感输入；普通数字计算输入同样会禁用翻译。草稿暂时只支持末尾输入/删除，没有完整光标编辑器。未加入拼音、语音、自动纠错、账号或订阅。

## 项目与构建

用 Android Studio 打开 `LazyTalkKeyboard` 目录，配置 SDK 后同步。已包含 Gradle Wrapper。

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

标准 APK：`app/build/outputs/apk/debug/app-debug.apk`。

本执行环境的 AAPT2 原生 ZIP 写入会崩溃，因此另附 `tools/build-windows-sandbox.ps1` 可选构建脚本，保留官方资源编译/链接，仅对其目录输出使用 .NET 封装 ZIP，不跳过资源或检查。一般 Android Studio 环境可先用标准命令。

完整结构、各文件职责、隐私模型、接口 JSON、构建说明及后续计划见 `README.md`。下一阶段从 Backend 的认证、额度和 AI Provider 接入继续；Android IME 不需要重写。
