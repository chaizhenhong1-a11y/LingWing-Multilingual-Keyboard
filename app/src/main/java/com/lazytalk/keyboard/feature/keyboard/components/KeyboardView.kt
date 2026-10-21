package com.lazytalk.keyboard.feature.keyboard.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.graphics.SolidColor
import kotlinx.coroutines.awaitCancellation
import androidx.compose.material3.*
import com.lazytalk.keyboard.ui.uiText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lazytalk.keyboard.core.model.*
import com.lazytalk.keyboard.feature.keyboard.presentation.*
import com.lazytalk.keyboard.feature.language.presentation.LanguagePicker

@Composable fun KeyboardView(model: KeyboardViewModel, onHide: () -> Unit, onSwitch: () -> Unit,
    onSelection: () -> Unit, onPaste: () -> Unit, onSettings: () -> Unit = {}, onRecognizeCopied: () -> Unit = {},
    onOpenRecognition: () -> Unit = model::openCopiedPanel) {
    val state by model.state.collectAsState()
    KeyboardContent(state, model::dispatch, onHide, onSwitch, onSelection, onPaste, model::refreshLanguages,
        onSettings = onSettings, onRecognizeCopied = onRecognizeCopied, onOpenRecognition = onOpenRecognition)
}

@Composable internal fun KeyboardContent(s: KeyboardUiState, action: (KeyboardAction) -> Unit,
    onHide: () -> Unit, onSwitch: () -> Unit, onSelection: () -> Unit, onPaste: () -> Unit,
    onRefresh: () -> Unit, onSettings: () -> Unit = {}, onRecognizeCopied: () -> Unit = {}, navigationInsets: WindowInsets = WindowInsets.navigationBars,
    onOpenRecognition: () -> Unit = onRecognizeCopied) {
    KeyFeedback(s.settings.keyVibration,durationMs=s.settings.keyVibrationStrength.durationMs) {
        KeyboardContentBody(s,action,onHide,onSwitch,onSelection,onPaste,onRefresh,onSettings,onRecognizeCopied,navigationInsets,onOpenRecognition)
    }
}

@Composable internal fun KeyboardContentBody(s: KeyboardUiState, action: (KeyboardAction) -> Unit,
    onHide: () -> Unit, onSwitch: () -> Unit, onSelection: () -> Unit, onPaste: () -> Unit,
    onRefresh: () -> Unit, onSettings: () -> Unit = {}, onRecognizeCopied: () -> Unit = {}, navigationInsets: WindowInsets = WindowInsets.navigationBars,
    onOpenRecognition: () -> Unit = onRecognizeCopied) {
    val config = LocalConfiguration.current
    val landscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
    val maxHeight = if (landscape) (config.screenHeightDp * 0.68f).coerceIn(236f, 280f).dp
        else (config.screenHeightDp * 0.52f).coerceIn(248f, 420f).dp
    val drafting = s.draftMode && !s.secure
    val reviewing = drafting && !s.liveTranslation && s.candidates.isNotEmpty()
    val toolbarHeight = if (drafting || landscape) 44.dp else 48.dp
    val inputHeight = if (landscape) 44.dp else 48.dp
    val statusHeight = if (s.error != null && s.composition.isEmpty()) 28.dp else 0.dp
    val baseKeyHeight = if (landscape) { if (drafting) 34.dp else 38.dp } else 48.dp
    val keyRowCount = if (s.page == KeyPage.LETTERS) s.typingLayout.tokenRows.size + 1 else 4
    val keyHeight = if (drafting) ((maxHeight - toolbarHeight - inputHeight - statusHeight) / keyRowCount - 2.dp)
        .coerceAtMost(baseKeyHeight).coerceAtLeast(24.dp) else minOf((baseKeyHeight * 4) / keyRowCount, (maxHeight - toolbarHeight) / keyRowCount - 2.dp)
    Surface(Modifier.testTag("keyboardWindow").windowInsetsPadding(navigationInsets), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.testTag("keyboard").fillMaxWidth().heightIn(max = maxHeight).padding(horizontal = 3.dp)) {
            if (s.layoutPicker) {
                TypingLayoutPicker(s.typingLayout, maxHeight,
                    { action(KeyboardAction.PickLayout(it)) }, { action(KeyboardAction.CloseLayouts) })
            } else if (s.copiedPanelOpen && !s.secure) {
                CopiedSentencePanel(s, action, Modifier.height(maxHeight), onPaste = onRecognizeCopied)
            } else if (s.picker != null) {
                LanguagePicker(s.languages, s.settings, s.picker == true,
                    { action(KeyboardAction.PickLanguage(it)) }, { action(KeyboardAction.Favorite(it)) },
                    { action(KeyboardAction.ClosePicker) }, Modifier.height(maxHeight), ime = true,
                    loading = s.languagesLoading, loadError = s.languageError, onRetry = onRefresh)
            } else {
                Row(Modifier.fillMaxWidth().height(toolbarHeight), verticalAlignment = Alignment.CenterVertically) {
                    if (s.composition.isNotEmpty() && !s.secure) {
                        TypingCandidateBar(s, action)
                    } else if (drafting) {
                        if (reviewing) TextButton(onClick = { action(KeyboardAction.Draft(s.draft)) },
                            contentPadding = PaddingValues(horizontal = 6.dp)) { Text(uiText("Edit")) }
                        else FilledTonalIconButton(onClick = { action(KeyboardAction.ToggleDraft) },
                            modifier = Modifier.size(40.dp).semantics { contentDescription = "Exit translation" }) { Text(uiText("←")) }
                        LanguageButton(uiText(languageName(s.source, s.languages)), Modifier.weight(1f)) { action(KeyboardAction.OpenPicker(true)) }
                        SwapButton { action(KeyboardAction.Swap) }
                        LanguageButton(languageName(s.target, s.languages), Modifier.weight(1f)) { action(KeyboardAction.OpenPicker(false)) }
                        DraftMenu(s, action, onSelection, onPaste, onSwitch, onRefresh, onSettings, onRecognizeCopied, onHide, onOpenRecognition)
                    } else {
                        NormalKeyboardToolbar(s, { action(KeyboardAction.ToggleDraft) }, onOpenRecognition,
                            onSettings, { action(KeyboardAction.OpenLayouts) })
                    }
                }
                if (reviewing) {
                    Column(Modifier.fillMaxWidth().height(minOf(maxHeight, 300.dp) - toolbarHeight).verticalScroll(rememberScrollState())) {
                        TranslationStatus(s)
                        CandidateList(s, action)
                    }
                } else {
                    if (drafting) {
                        Row(Modifier.fillMaxWidth().height(inputHeight), verticalAlignment = Alignment.CenterVertically) {
                            ToolButton("▣", "打开识别列表", onOpenRecognition)
                            DraftPreview(s, action, Modifier.weight(1f))
                            ConvertButton(s) { action(KeyboardAction.Translate) }
                        }
                        if (statusHeight > 0.dp) Text(s.error.orEmpty(),
                            Modifier.fillMaxWidth().height(statusHeight).padding(horizontal = 6.dp),
                            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    KeyboardRows(s, action, keyHeight = keyHeight, onSwitch = onSwitch)
                    if (!drafting) s.replyLanguageNotice?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

@Composable private fun LanguageButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick, modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
        Text(uiText("$label ▾"), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
@Composable private fun ToolButton(label: String, description: String, onClick: () -> Unit) {
    TextButton(onClick, Modifier.width(44.dp).semantics { contentDescription = description }, contentPadding = PaddingValues(0.dp)) { Text(uiText(label)) }
}
@Composable private fun SwapButton(onClick: () -> Unit) = ToolButton("⇄", "Swap source and target languages", onClick)

@Composable private fun DraftMenu(s: KeyboardUiState, action: (KeyboardAction) -> Unit, onSelection: () -> Unit,
    onPaste: () -> Unit, onSwitch: () -> Unit, onRefresh: () -> Unit, onSettings: () -> Unit, onRecognizeCopied: () -> Unit, onHide: () -> Unit,
    onOpenRecognition: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        ToolButton("⋯", "Draft options", { open = true })
        DropdownMenu(open, { open = false }) {
            com.lazytalk.keyboard.ui.InterfaceLanguageButton()
            DropdownMenuItem(text = { Text((if (s.liveTranslation) "✓ " else "") + uiText("实时翻译")) },
                onClick = { open = false; action(KeyboardAction.ToggleLiveTranslation) })
            HorizontalDivider()
            Tone.entries.forEach { tone ->
                DropdownMenuItem(text = { Text((if (tone == s.tone) "✓ " else "") + "Tone · " + tone.wire) },
                    onClick = { open = false; action(KeyboardAction.ToneChanged(tone)) })
            }
            HorizontalDivider()
            listOf("识别列表" to onOpenRecognition, "粘贴对方的话（自动识别）" to onRecognizeCopied, "Use selected text" to onSelection, "Paste my reply · 粘贴我的原文" to onPaste,
                "Clear draft" to { action(KeyboardAction.Clear) }, "Switch keyboard" to onSwitch,
                "Exit translation" to { action(KeyboardAction.ToggleDraft) }, "Hide keyboard" to onHide,
                "Refresh languages" to onRefresh, "Keyboard settings" to onSettings).forEach { (label, run) ->
                DropdownMenuItem(text = { Text(uiText(label)) }, onClick = { open = false; run() })
            }
        }
    }
}

@Composable private fun ConvertButton(s: KeyboardUiState, modifier: Modifier = Modifier, onClick: () -> Unit) {
    if (s.liveTranslation) {
        val description = when {
            s.liveBlocked -> "自动写入已暂停"
            s.loading -> "正在翻译"
            s.error != null -> "重试翻译"
            s.settings.mode == TranslationMode.MOCK -> "演示模式，实时翻译未连接"
            else -> "实时翻译已开启"
        }
        IconButton(onClick, modifier.size(36.dp).semantics { contentDescription = description },
            enabled = !s.loading && !s.liveBlocked && s.draft.isNotBlank() && s.settings.mode == TranslationMode.REMOTE) {
            when {
                s.loading -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 1.5.dp)
                s.error != null || s.liveBlocked -> Text(uiText("!"), style = MaterialTheme.typography.labelMedium)
                else -> Box(Modifier.size(6.dp).background(
                    if (s.settings.mode == TranslationMode.REMOTE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    CircleShape))
            }
        }
        return
    }
    Button(onClick, modifier.height(40.dp), enabled = !s.loading && s.draft.isNotBlank(), contentPadding = PaddingValues(horizontal = 12.dp)) {
        Text(uiText(if (s.loading) "Working…" else if (s.error != null || s.candidates.isNotEmpty()) "Retry" else "Convert"), maxLines = 1)
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable private fun DraftPreview(s: KeyboardUiState, action: (KeyboardAction) -> Unit, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    val hostWindow = LocalWindowInfo.current
    // An IME window deliberately does not take Android window focus away from the host app.
    // Compose gates its cursor on that flag, so give only this active internal editor logical focus.
    // Other window properties and the real WindowManager flags remain those of the host window.
    val editorWindow = remember(hostWindow) { object : WindowInfo by hostWindow {
        override val isWindowFocused: Boolean get() = true
    } }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Surface(modifier.padding(horizontal = 4.dp, vertical = 4.dp).fillMaxHeight(),
        shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        // The draft is edited by our own keys; never start another system IME inside this IME.
        InterceptPlatformTextInput(interceptor = { _, _ -> awaitCancellation() }) {
          CompositionLocalProvider(LocalWindowInfo provides editorWindow) {
            BasicTextField(value = TextFieldValue(s.draft, s.draftSelection),
                onValueChange = { action(KeyboardAction.EditDraft(it.text, it.selection)) },
                modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("draftEditor")
                    .semantics { contentDescription = "翻译原文输入框" }.padding(horizontal = 12.dp, vertical = 8.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), maxLines = 2,
                decorationBox = { editor ->
                    Box { if (s.draft.isEmpty()) Text(uiText("输入回复"), color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium); editor() }
                })
          }
        }
    }
}

@Composable private fun TranslationStatus(s: KeyboardUiState) {
    s.detected?.let { Text(uiText("Detected: ${languageName(it, s.languages)}"), style = MaterialTheme.typography.labelSmall) }
    s.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }
    if (s.settings.mode == TranslationMode.MOCK && s.candidates.isNotEmpty())
        Text(uiText("Mock samples · tones unchanged"), style = MaterialTheme.typography.labelSmall)
}
@Composable private fun CandidateList(s: KeyboardUiState, action: (KeyboardAction) -> Unit) {
    s.candidates.forEach { text ->
        OutlinedButton(onClick = { action(KeyboardAction.Candidate(text)) }, modifier = Modifier.fillMaxWidth()) {
            Text(text)
        }
    }
}
@Composable private fun languageName(code: String, languages: List<Language>): String {
    if(code=="auto") return uiText("Auto Detect")
    val language=languages.find {it.code==code}
    val locale=LocalConfiguration.current.locales[0]
    return java.util.Locale.forLanguageTag(code).getDisplayLanguage(locale).takeIf {it.isNotBlank() && it!=code}
        ?: language?.name ?: code
}
