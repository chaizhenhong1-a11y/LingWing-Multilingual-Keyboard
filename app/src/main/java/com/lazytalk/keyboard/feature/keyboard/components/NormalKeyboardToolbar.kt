package com.lazytalk.keyboard.feature.keyboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.lazytalk.keyboard.ui.uiText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.lazytalk.keyboard.feature.keyboard.presentation.KeyboardUiState

private fun toolbarIcon(name: String, path: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    .addPath(PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black)).build()

private val translateIcon = toolbarIcon("Translate", "M12.87,15.07L10.33,12.56L10.36,12.53C12.1,10.59 13.34,8.36 14.07,6L17,6L17,4L10,4L10,2L8,2L8,4L1,4L1,6L12.17,6C11.5,7.92 10.44,9.75 9,11.35C8.07,10.32 7.3,9.19 6.69,8L4.69,8C5.42,9.63 6.42,11.17 7.67,12.56L2.58,17.58L4,19L9,14L12.11,17.11Z M18.5,10L16.5,10L12,22L14,22L15.12,19L19.87,19L21,22L23,22Z M15.88,17L17.5,12.67L19.12,17Z")
private val clipboardIcon = toolbarIcon("Clipboard", "M16,3H20V22H4V3H8V2H16Z M6,5V20H18V5H16V7H8V5Z M10,4V5H14V4Z M8,10H16V12H8Z M8,14H16V16H8Z")
private val settingsIcon = toolbarIcon("Settings", "M10,2H14L15,5L17,6L20,5L22,9L20,11V13L22,15L20,19L17,18L15,19L14,22H10L9,19L7,18L4,19L2,15L4,13V11L2,9L4,5L7,6L9,5Z M12,8A4,4 0,1 0,12,16A4,4 0,1 0,12,8Z")

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ToolbarIcon(icon: ImageVector, description: String, tip: String, enabled: Boolean, click: () -> Unit) {
    TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(uiText(tip)) } }, state = rememberTooltipState()) {
        IconButton(click, enabled = enabled) { Icon(icon, description, Modifier.size(24.dp)) }
    }
}

@Composable internal fun RowScope.NormalKeyboardToolbar(s: KeyboardUiState, translate: () -> Unit,
    clipboard: () -> Unit, settings: () -> Unit, layouts: () -> Unit) {
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        ToolbarIcon(translateIcon, if (s.secure) "Translation unavailable in secure fields" else "Translate", "翻译", !s.secure, translate)
    }
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        ToolbarIcon(clipboardIcon, "打开剪贴板页面", "剪贴板", !s.secure, clipboard)
    }
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        ToolbarIcon(settingsIcon, "Keyboard settings", "设置", true, settings)
    }
    FilledTonalButton(layouts, Modifier.weight(1.25f).height(36.dp).semantics { contentDescription = "当前键盘语言：${s.typingLayout.name}" },
        contentPadding = PaddingValues(horizontal = 8.dp)) {
        Text(com.lazytalk.keyboard.ui.layoutDisplayName(s.typingLayout) + " ▾", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
