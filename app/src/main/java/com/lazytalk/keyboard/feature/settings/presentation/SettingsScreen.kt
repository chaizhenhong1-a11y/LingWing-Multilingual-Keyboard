package com.lazytalk.keyboard.feature.settings.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import com.lazytalk.keyboard.ui.InterfaceLanguage
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.lazytalk.keyboard.ui.uiText
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lazytalk.keyboard.BuildConfig
import com.lazytalk.keyboard.core.model.*
import com.lazytalk.keyboard.feature.language.presentation.LanguagePicker
import com.lazytalk.keyboard.core.network.BackendUrlPolicy

@Composable fun SettingsScreen(model: SettingsViewModel, onBack: () -> Unit, initialPage: String = "home",
    keyboardEnabled: Boolean = false, keyboardSelected: Boolean = false,
    onEnableKeyboard: () -> Unit = {}, onSelectKeyboard: () -> Unit = {}) {
    val context=LocalContext.current
    val interfaceLanguage=InterfaceLanguage.selected(context)
    val s by model.settings.collectAsState()
    val languages by model.languages.collectAsState()
    val message by model.message.collectAsState()
    var picker by remember { mutableStateOf(false) }
    var url by remember(s.baseUrl) { mutableStateOf(s.baseUrl) }
    var token by remember(s.backendToken) { mutableStateOf(s.backendToken) }
    var invalid by remember { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf(initialPage) }
    var accountOpen by remember { mutableStateOf(false) }
    val back: () -> Unit = { if(page=="home") onBack() else page="home" }
    BackHandler { if(accountOpen) accountOpen=false else if(picker) picker=false else back() }
    if(accountOpen) AccountScreen(model) {accountOpen=false}
    else if(picker) LanguagePicker(languages,s,false,{code -> model.update {it.copy(target=code)};picker=false},
        model::favorite,{picker=false},Modifier.fillMaxSize(),onRetry=model::refresh)
    else Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
                if(page!="home") IconButton(onClick=back) { Text(uiText("←"),style=MaterialTheme.typography.headlineSmall) }
                else { AccountMark(Modifier.size(32.dp)); Spacer(Modifier.width(12.dp)) }
                Text(uiText(when(page) {"interface"->"界面语言";"translation"->"翻译设置";"languages"->"语言记录";"privacy"->"隐私";"about"->"关于";"developer"->"开发连接";else->"LingWing Keyboard"}),
                    style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
                com.lazytalk.keyboard.ui.InterfaceLanguageButton {page="interface"}
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=8.dp),
                verticalArrangement=Arrangement.spacedBy(8.dp)) {
                when(page) {
                    "home" -> {
                        AccountCard(model) {accountOpen=true}
                        SettingsSection("键盘")
                        SettingsEntry("Enable Keyboard",if(keyboardEnabled) "已启用" else "尚未启用，请先开启 LingWing Keyboard") {onEnableKeyboard()}
                        SettingsEntry("Select Keyboard",if(keyboardSelected) "正在使用 LingWing Keyboard" else "切换到 LingWing Keyboard 输入法") {onSelectKeyboard()}
                        KeyVibrationSetting(s.keyVibration,s.keyVibrationStrength.durationMs) { enabled -> model.update {it.copy(keyVibration=enabled)} }
                        if(s.keyVibration) KeyVibrationStrengthSetting(s.keyVibrationStrength) { strength -> model.update {it.copy(keyVibrationStrength=strength)} }
                        SettingsSection("常用")
                        SettingsEntry("界面语言",when(interfaceLanguage) {"zh"->"简体中文";"en"->"English";else->"跟随系统"}) {page="interface"}
                        SettingsEntry("翻译设置","目标语言与表达语气") {page="translation"}
                        SettingsEntry("语言记录","管理最近使用的翻译语言") {page="languages"}
                        SettingsSection("更多")
                        SettingsEntry("隐私","了解翻译内容与数据处理") {page="privacy"}
                        SettingsEntry("关于 LingWing Keyboard","${uiText("版本")} ${BuildConfig.VERSION_NAME}") {page="about"}
                        if(BuildConfig.DEBUG) SettingsEntry("开发连接","仅供调试使用") {page="developer"}
                    }
                    "interface" -> {
                        SettingsSection("选择界面显示语言")
                        listOf("" to "跟随系统","zh" to "简体中文","en" to "English").forEach { (tag,label) ->
                            val choose: () -> Unit = {
                                var host=context
                                while(host is android.content.ContextWrapper && host !is android.app.Activity) host=host.baseContext
                                (host as? android.app.Activity)?.let {InterfaceLanguage.select(it,tag)}
                            }
                            Row(Modifier.fillMaxWidth().clickable(onClick=choose).padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
                                RadioButton(selected=interfaceLanguage==tag,onClick=choose)
                                Text(uiText(label))
                            }
                        }
                        Text(uiText("仅更改应用界面，不改变键盘输入语言或翻译目标语言。"),style=MaterialTheme.typography.bodySmall)
                    }
                    "translation" -> {
                        SettingsSection("翻译偏好")
                        SettingsEntry("默认目标语言",languages.find {it.code==s.target}?.nativeName ?: s.target) {picker=true}
                        SettingsSection("表达语气")
                        Tone.entries.forEach {tone ->
                            Row(Modifier.fillMaxWidth().clickable {model.update {it.copy(tone=tone)}}.padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
                                RadioButton(selected=s.tone==tone,onClick={model.update {it.copy(tone=tone)}})
                                Text(uiText(when(tone.wire) {"natural"->"自然";"casual"->"随意";"polite"->"礼貌";"short"->"简短";else->tone.wire}))
                            }
                        }
                    }
                    "languages" -> {
                        SettingsEntry("清空最近使用","${s.recent.size} ${uiText("种语言")}") {model.update {it.copy(recent=emptyList())}}
                        SettingsEntry("刷新语言列表","重新获取可用的翻译语言") {model.refresh()}
                        message?.let {Text(uiText(it),style=MaterialTheme.typography.bodySmall)}
                    }
                    "privacy" -> {
                        SettingsSection("翻译内容")
                        Text(uiText("点击转换或开启实时翻译后，待翻译文字会发送至配置的后端及翻译服务。普通输入不会发送至翻译服务。"))
                        SettingsSection("本机数据")
                        Text(uiText("输入草稿和翻译结果保留在内存中，输入会话结束后清除。复制到识别列表的句子保存在本机，可在剪贴板页面清空。"))
                        SettingsSection("敏感输入")
                        Text(uiText("密码、数字、电话号码以及要求禁止个性化学习的输入框会禁用翻译。应用未正确标记的敏感文本无法可靠识别，请在翻译前检查内容。"))
                    }
                    "about" -> {
                        AccountMark(Modifier.size(52.dp))
                        Text(uiText("LingWing Keyboard"),style=MaterialTheme.typography.headlineSmall)
                        Text(uiText("${uiText("版本")} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"),color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(uiText("多语言输入与 AI 翻译。"),style=MaterialTheme.typography.bodyLarge)
                    }
                    "developer" -> {
            Text(uiText("Translation Mode · Development build"))
            Button(onClick = {
                url = "http://127.0.0.1:8787"
                model.update { it.copy(mode = TranslationMode.REMOTE, baseUrl = url, backendToken = "") }
            }) { Text(uiText("Connect OpenAI via USB")) }
            Text(uiText("Start the computer backend and run adb reverse tcp:8787 tcp:8787. Keep USB connected during testing."))
            if(BuildConfig.CLOUD_BACKEND_URL.isNotBlank()) OutlinedButton(onClick = {
                url=BuildConfig.CLOUD_BACKEND_URL;invalid=false
            }) { Text(uiText("使用 Google Cloud 连接")) }
            Row { TranslationMode.entries.forEach { mode ->
                TextButton(onClick = { model.update { it.copy(mode = mode) } }) { Text(uiText((if (s.mode == mode) "✓ " else "") + mode.name)) }
            } }
            OutlinedTextField(url, { url = it; invalid = false }, label = { Text(uiText("Backend URL (HTTPS or USB localhost)")) },
                singleLine = true, isError = invalid, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(token, { token=it.trim();invalid=false }, label={ Text(uiText("云端访问码（不是 OpenAI 密钥）")) },
                visualTransformation=PasswordVisualTransformation(), singleLine=true, modifier=Modifier.fillMaxWidth())
            Button(onClick = {
                if(BackendUrlPolicy.isValid(url.trim()) && (url.trim().startsWith("http://127.0.0.1:") || token.matches(Regex("lt_[A-Za-z0-9_-]{32,128}"))))
                    model.update { it.copy(baseUrl=url.trim(), backendToken=if(url.trim().startsWith("https://")) token else "", mode=TranslationMode.REMOTE) }
                else invalid = true
            }) { Text(uiText("保存连接并启用翻译")) }
            if (invalid) Text(uiText("请输入有效 HTTPS 地址和云端访问码；USB 测试可用 http://127.0.0.1:8787。"), color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable private fun SettingsSection(title: String) {
    Text(uiText(title),Modifier.padding(top=16.dp,bottom=4.dp),color=MaterialTheme.colorScheme.primary,
        style=MaterialTheme.typography.labelLarge)
}

@Composable private fun SettingsEntry(title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=14.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(uiText(title),style=MaterialTheme.typography.titleMedium)
            Text(uiText(subtitle),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(uiText("›"),style=MaterialTheme.typography.headlineSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable internal fun KeyVibrationSetting(enabled: Boolean, durationMs: Int = 30, onChange: (Boolean) -> Unit) {
    val context=LocalContext.current
    val motor=remember(context) {com.lazytalk.keyboard.feature.keyboard.components.KeyVibration(context)}
    val change: (Boolean) -> Unit = {value -> onChange(value);if(value) motor.pulse(durationMs)}
    Row(Modifier.fillMaxWidth().toggleable(value=enabled,role=Role.Switch,onValueChange=change).padding(vertical=14.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(uiText("按键震动"),style=MaterialTheme.typography.titleMedium)
            Text(uiText("打字时提供触感反馈，遵循系统触感设置"),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked=enabled,onCheckedChange=null)
    }
}

@Composable internal fun KeyVibrationStrengthSetting(value: KeyVibrationStrength, onChange: (KeyVibrationStrength) -> Unit) {
    val context=LocalContext.current
    val motor=remember(context) {com.lazytalk.keyboard.feature.keyboard.components.KeyVibration(context)}
    Column(Modifier.fillMaxWidth().padding(bottom=8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(uiText("震动强度"),style=MaterialTheme.typography.titleSmall)
        Text(uiText("选择后立即试震"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            KeyVibrationStrength.entries.forEach {strength ->
                FilterChip(selected=value==strength,onClick={onChange(strength);motor.pulse(strength.durationMs)},
                    label={Text(uiText(when(strength) {KeyVibrationStrength.LIGHT->"轻";KeyVibrationStrength.STANDARD->"标准";KeyVibrationStrength.STRONG->"强"}))},modifier=Modifier.weight(1f))
            }
        }
    }
}
