package com.lazytalk.keyboard.feature.keyboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.lazytalk.keyboard.ui.uiText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lazytalk.keyboard.core.model.*
import com.lazytalk.keyboard.feature.keyboard.presentation.*
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

@Composable internal fun TypingLayoutPicker(current: TypingLayout, height: Dp, select: (String) -> Unit, close: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val wide=LocalConfiguration.current.orientation==Configuration.ORIENTATION_LANDSCAPE
    Column(Modifier.fillMaxWidth().height(height).padding(horizontal = 8.dp)) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(uiText("选择键盘语言"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            com.lazytalk.keyboard.ui.InterfaceLanguageButton()
            TextButton(close) { Text(uiText("返回键盘")) }
        }
        val search: @Composable () -> Unit = {
            OutlinedTextField(query, {}, Modifier.fillMaxWidth().padding(bottom = 6.dp), readOnly=true,
                placeholder = { Text(uiText("搜索语言 / Search languages")) }, singleLine = true)
        }
        val matches = TypingLayouts.all.filter { query.isBlank() ||
            listOf(it.label, it.id, it.variant, java.util.Locale.forLanguageTag(it.tag).getDisplayName(java.util.Locale.ENGLISH)).any { value -> value.contains(query.trim(), true) } }
        if (matches.isEmpty()) Text(uiText("没有匹配的键盘语言"), Modifier.padding(12.dp))
        val list: @Composable (Modifier) -> Unit = { modifier -> LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
            items(matches, key = { it.id }) { layout ->
                val selected = layout.id == current.id
                Surface(onClick = { select(layout.id) }, shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(com.lazytalk.keyboard.ui.layoutDisplayName(layout) + " · " + layout.name, style = MaterialTheme.typography.titleSmall)
                            Text(uiText(if(layout.id=="th") "泰文按键 · Kedmanee" else if(layout.variant.isNotBlank()) layout.variant.replace('_',' ') else "Switch to this keyboard"), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (selected) Text(uiText("✓ 当前"), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        } }
        val keys: @Composable () -> Unit = {
            KeyboardRows(KeyboardUiState(enterLabel="Clear"), { event ->
                when(event) {
                    is KeyboardAction.Key -> query=(query+event.text).take(60)
                    KeyboardAction.Backspace -> query=query.dropLast(1)
                    KeyboardAction.Enter -> query=""
                    else -> Unit
                }
            }, keyHeight=if(wide) 36.dp else 42.dp)
        }
        if(wide) Row(Modifier.weight(1f)) {
            Column(Modifier.weight(0.5f)) { search();list(Modifier.weight(1f)) }
            Column(Modifier.weight(0.5f), verticalArrangement=Arrangement.Center) { keys() }
        } else { search();list(Modifier.weight(1f));keys() }
    }
}
