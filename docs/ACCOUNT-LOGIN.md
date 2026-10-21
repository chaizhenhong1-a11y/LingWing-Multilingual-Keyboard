# LazyTalk 0.4.0 账号与额度

## 0.4.1 免费不限次数账号与登录页面

用户已确认 businessstanley8@gmail.com 获得免费不限翻译次数权限。权限绑定该邮箱现有 Identity Platform 用户的不可变 UID，保存在 Cloud Run 的 UNLIMITED_USER_IDS 服务器配置中，不信任客户端提交的邮箱、套餐或 unlimited 字段。该账号不受每日个人 50 次、全站 500 次及个人每分钟 10 次的计数额度限制，也不消耗普通账号的全站每日次数。仍保留身份/邮箱验证、单次 500 字符、并发租约和服务端基本流量保护。

`/account` 对该账号返回 unlimited=true、plan=owner_free，limit 和 remaining 均为 null；普通账号的限制不变。未接入任何支付、订阅或自动扣款。免费指 App 内没有收费，OpenAI 和云服务的实际调用仍由项目账单承担。

登录表单已从设置卡片拆成独立页面，包含 Google 入口、邮箱密码、显示/隐藏密码、注册切换、密码重置和账号状态；设置里只保留紧凑账号入口。不限次数账号显示“免费 · 不限翻译次数”。

普通打字、离线拼音/假名/韩文及布局切换不要求登录。云端翻译改为验证 Google Identity Platform 的个人身份令牌，不再接受旧的共享访问码。项目为 `holysht`，服务为 `lazytalk-api`。

## 登录

在 App 首页打开 Keyboard Settings，账号卡片支持邮箱注册、邮箱密码登录、验证邮件重发、验证状态刷新、密码重置、退出及剩余额度刷新。注册需要至少 8 位密码，验证邮箱后才允许调用 OpenAI。密码直接发往 Google Identity Toolkit，不经过 LazyTalk 后端，不在手机持久化。手机仅将刷新令牌用 Android Keystore AES-GCM 加密存储，短期 ID token 保存在内存，按需续期。备份保持关闭。退出清除本地会话和 Credential Manager 状态；这不会退出手机的 Google 系统账号。

Google 登录使用 Android Credential Manager 和 Google ID SDK，将 Google ID token 交换成同一 Identity Platform 项目的个人会话。只允许 password/google.com 提供方访问翻译，匿名令牌被拒绝。同一个 Firebase UID 共用同一份额度；不同身份不能仅凭客户端传来的邮箱合并。遇到已有账号冲突时应先使用原登录方式。

Google 登录提供方和公开支持邮箱已配置：应用名称 LazyTalk Keyboard，支持邮箱 businessstanley8@gmail.com。该账号经用户确认获得项目的 OAuth Config Editor 权限，未授予 Owner。Android 包名及交付 APK 的 SHA-1/SHA-256 已登记，生成的 Android OAuth client 和 web client ID 与提供方配置核对一致。公开 OAuth web client ID 从云端 `/auth-config` 获取，无需让用户填配置，也无需把 OpenAI 密钥或共享访问码打进 APK。用于 Google 认证的 public API key 仅限制到 Identity Toolkit 和 Secure Token API，它不是 OpenAI 密钥。

## 当前试运营限额

- 已验证账号每天 50 次，每次最多 500 个 Unicode 码点（Android 输入长度显示可能不同）。
- 每账号 60 秒内最多 10 次；同一账号同时只能有一次正在处理的请求。
- 全站每天最多 500 次。此值是请求数保护，不是 US$5 金额封顶。
- UTC 00:00 重置，即马来西亚时间早上 08:00。
- 实时翻译每次实际后端调用都计数。参数校验失败不扣额度；请求进入上游处理后即计数，即使上游失败也不退款，以免重试绕过成本保护。
- Firestore 事务在调用上游之前同时预留个人额度、全站额度和并发租约。数据库不可用时停止调用 OpenAI，重装和容器重启不能重置额度。
- 额度库仅存 UID 的 SHA-256 标识、日期、计数、短期请求时间和并发租约，不保存聊天原文、密码或翻译结果。

数据库为独立的 `lazytalk-usage`。运行身份仅获得此数据库的条件化 Datastore User 权限，以及检查禁用/撤销账号所需的 Firebase Authentication Viewer 权限。每次受保护请求校验 token 的签名、项目和撤销/禁用状态。

## 运维

Cloud Run 环境变量 `USER_DAILY_LIMIT`、`GLOBAL_DAILY_LIMIT` 控制次数，`GOOGLE_CLIENT_ID` 控制 Google 登录 web client ID。修改前核对现有服务配置，使用 `--update-env-vars` 保留 OpenAI 模型及 AUTH_PROJECT_ID。Secret Manager 只向运行时提供 OPENAI_API_KEY，客户端无权读取。

这是试运营账号/额度保护。尚未集成 Play Integrity/App Check、付费订阅、匿名免费试用、精确货币预算和风控审核；注册很多真实邮箱仍可能消耗全站额度。正式大规模发布前需要补足这些措施及隐私政策/账号删除流程。需要实际 HONOR 手机验收 Keystore、验证邮件点击和 Google 账号选择器；单元测试不能代替它们。

参考：
- https://firebase.google.com/docs/reference/rest/auth
- https://firebase.google.com/docs/auth/admin/verify-id-tokens
- https://firebase.google.com/docs/firestore/manage-data/transactions
- https://developer.android.com/identity/sign-in/credential-manager-siwg
- https://support.google.com/cloud/answer/15549049
