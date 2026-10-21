package com.lazytalk.keyboard.ui
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lazytalk.keyboard.R

@Composable fun uiText(text: String): String {
    val id=when(text) {
        "设置" -> R.string.ui_0
        "常用" -> R.string.ui_1
        "更多" -> R.string.ui_2
        "翻译设置" -> R.string.ui_3
        "目标语言与表达语气" -> R.string.ui_4
        "语言记录" -> R.string.ui_5
        "管理最近使用的翻译语言" -> R.string.ui_6
        "隐私" -> R.string.ui_7
        "了解翻译内容与数据处理" -> R.string.ui_8
        "关于 LingWing Keyboard" -> R.string.ui_9
        "关于" -> R.string.ui_10
        "开发连接" -> R.string.ui_11
        "仅供调试使用" -> R.string.ui_12
        "翻译偏好" -> R.string.ui_13
        "默认目标语言" -> R.string.ui_14
        "表达语气" -> R.string.ui_15
        "自然" -> R.string.ui_16
        "随意" -> R.string.ui_17
        "礼貌" -> R.string.ui_18
        "简短" -> R.string.ui_19
        "清空最近使用" -> R.string.ui_20
        "刷新语言列表" -> R.string.ui_21
        "重新获取可用的翻译语言" -> R.string.ui_22
        "翻译内容" -> R.string.ui_23
        "本机数据" -> R.string.ui_24
        "敏感输入" -> R.string.ui_25
        "多语言输入与 AI 翻译。" -> R.string.ui_26
        "界面语言" -> R.string.ui_27
        "跟随系统" -> R.string.ui_28
        "选择界面显示语言" -> R.string.ui_29
        "仅更改应用界面，不改变键盘输入语言或翻译目标语言。" -> R.string.ui_30
        "点击转换或开启实时翻译后，待翻译文字会发送至配置的后端及翻译服务。普通输入不会发送至翻译服务。" -> R.string.ui_31
        "输入草稿和翻译结果保留在内存中，输入会话结束后清除。复制到识别列表的句子保存在本机，可在剪贴板页面清空。" -> R.string.ui_32
        "密码、数字、电话号码以及要求禁止个性化学习的输入框会禁用翻译。应用未正确标记的敏感文本无法可靠识别，请在翻译前检查内容。" -> R.string.ui_33
        "登录 LingWing Keyboard" -> R.string.ui_34
        "我的账号" -> R.string.ui_35
        "登录后开启 AI 翻译" -> R.string.ui_36
        "免费 · 不限次数" -> R.string.ui_37
        "欢迎使用 LingWing Keyboard" -> R.string.ui_38
        "创建你的账号" -> R.string.ui_39
        "让每一种语言，都聊得自然。" -> R.string.ui_40
        "使用 Google 账号继续" -> R.string.ui_41
        "或使用邮箱" -> R.string.ui_42
        "邮箱地址" -> R.string.ui_43
        "密码" -> R.string.ui_44
        "至少 8 位字符" -> R.string.ui_45
        "忘记密码？" -> R.string.ui_46
        "登录" -> R.string.ui_47
        "创建账号" -> R.string.ui_48
        "已有账号？立即登录" -> R.string.ui_49
        "还没有账号？免费注册" -> R.string.ui_50
        "免费 · 不限翻译次数" -> R.string.ui_51
        "免费方案" -> R.string.ui_52
        "尽情表达，自在交流。" -> R.string.ui_53
        "每次最多 500 字符。" -> R.string.ui_54
        "每次最多 500 字符。额度每日早上 8 点（UTC+8）恢复。" -> R.string.ui_55
        "验证邮箱，即可开启翻译" -> R.string.ui_56
        "打开验证邮件中的链接，然后回来确认。" -> R.string.ui_57
        "我已验证邮箱" -> R.string.ui_58
        "重新发送验证邮件" -> R.string.ui_59
        "刷新账号状态" -> R.string.ui_60
        "退出登录" -> R.string.ui_61
        "普通输入无需登录。仅在使用翻译时发送翻译内容。" -> R.string.ui_62
        "正在获取额度" -> R.string.ui_63
        "Global AI multilingual keyboard" -> R.string.ui_64
        "Enable Keyboard" -> R.string.ui_65
        "Select Keyboard" -> R.string.ui_66
        "Keyboard Settings" -> R.string.ui_67
        "Type with live translation" -> R.string.ui_68
        "Try the actual system keyboard here" -> R.string.ui_69 
        "版本" -> R.string.ui_version
        "种语言" -> R.string.ui_language_count
        "选择键盘语言" -> R.string.keyboard_ui_0
        "返回键盘" -> R.string.keyboard_ui_1
        "搜索语言 / Search languages" -> R.string.keyboard_ui_2
        "没有匹配的键盘语言" -> R.string.keyboard_ui_3
        "✓ 当前" -> R.string.keyboard_ui_4
        "剪贴板" -> R.string.keyboard_ui_5
        "粘贴" -> R.string.keyboard_ui_6
        "清空" -> R.string.keyboard_ui_7
        "完成" -> R.string.keyboard_ui_8
        "目标：" -> R.string.keyboard_ui_9
        "正在本地识别…" -> R.string.keyboard_ui_10
        "识别这句并设置回复语言" -> R.string.keyboard_ui_11
        "输入回复" -> R.string.keyboard_ui_12
        "Auto Detect" -> R.string.keyboard_ui_13
        "Choose language" -> R.string.keyboard_ui_14
        "Close" -> R.string.keyboard_ui_15
        "Search Languages" -> R.string.keyboard_ui_16
        "Results" -> R.string.keyboard_ui_17
        "Recent" -> R.string.keyboard_ui_18
        "Favorites" -> R.string.keyboard_ui_19
        "Popular" -> R.string.keyboard_ui_20
        "All Languages" -> R.string.keyboard_ui_21
        "No matching languages" -> R.string.keyboard_ui_22
        "Retry connection" -> R.string.keyboard_ui_23
        "Loading languages…" -> R.string.keyboard_ui_24
        "Language list is empty. Refresh to try again." -> R.string.keyboard_ui_25
        "Retry language list" -> R.string.keyboard_ui_26
        "Edit" -> R.string.keyboard_ui_27
        "Working…" -> R.string.keyboard_ui_28
        "Retry" -> R.string.keyboard_ui_29
        "Convert" -> R.string.keyboard_ui_30
        "实时翻译" -> R.string.keyboard_ui_31
        "识别列表" -> R.string.keyboard_ui_32
        "粘贴对方的话（自动识别）" -> R.string.keyboard_ui_33
        "Use selected text" -> R.string.keyboard_ui_34
        "Paste my reply · 粘贴我的原文" -> R.string.keyboard_ui_35
        "Clear draft" -> R.string.keyboard_ui_36
        "Switch keyboard" -> R.string.keyboard_ui_37
        "Exit translation" -> R.string.keyboard_ui_38
        "Hide keyboard" -> R.string.keyboard_ui_39
        "Refresh languages" -> R.string.keyboard_ui_40
        "Keyboard settings" -> R.string.keyboard_ui_41
        "翻译" -> R.string.keyboard_ui_42
        "打开剪贴板页面" -> R.string.keyboard_ui_43
        "Switch to this keyboard" -> R.string.keyboard_ui_44
        "全拼 · 候选选字" -> R.string.keyboard_ui_45
        "假名 · 候选选词" -> R.string.keyboard_ui_46
        "双拼 · 韩文组合" -> R.string.keyboard_ui_47
        "泰文按键 · Kedmanee" -> R.string.keyboard_ui_48
        "键盘" -> R.string.setup_keyboard
        "已启用" -> R.string.setup_enabled
        "尚未启用，请先开启 LingWing Keyboard" -> R.string.setup_disabled
        "正在使用 LingWing Keyboard" -> R.string.setup_selected
        "切换到 LingWing Keyboard 输入法" -> R.string.setup_select
        "按键震动" -> R.string.key_vibration
        "打字时提供触感反馈，遵循系统触感设置" -> R.string.key_vibration_description
        "震动强度" -> R.string.key_vibration_strength
        "选择后立即试震" -> R.string.key_vibration_preview
        "轻" -> R.string.key_vibration_light
        "标准" -> R.string.key_vibration_standard
        "强" -> R.string.key_vibration_strong
        else -> null
    }
    return id?.let {stringResource(it)} ?: text
}
