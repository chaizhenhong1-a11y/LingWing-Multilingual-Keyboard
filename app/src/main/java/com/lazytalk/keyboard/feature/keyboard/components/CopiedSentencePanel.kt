package com.lazytalk.keyboard.feature.keyboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import com.lazytalk.keyboard.ui.uiText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lazytalk.keyboard.feature.keyboard.presentation.*

@Composable internal fun CopiedSentencePanel(s: KeyboardUiState, action: (KeyboardAction) -> Unit, modifier: Modifier = Modifier,
    onPaste: () -> Unit = {}) {
    Column(modifier.padding(horizontal = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(uiText("剪贴板"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            com.lazytalk.keyboard.ui.InterfaceLanguageButton()
            TextButton(onClick = onPaste) { Text(uiText("粘贴")) }
            TextButton(onClick = { action(KeyboardAction.ClearCopiedSentences) }) { Text(uiText("清空")) }
            TextButton(onClick = { action(KeyboardAction.CloseCopiedSentence) }) { Text(uiText("完成")) }
        }
        Text(uiText("目标：") + (s.languages.find { it.code == s.target }?.nativeName ?: s.target),
            style = MaterialTheme.typography.labelMedium)
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(s.copiedSentences) { index, sentence ->
                Row(Modifier.fillMaxWidth().clickable { action(KeyboardAction.SelectCopiedSentence(index)) }
                    .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = index == s.selectedCopiedSentence,
                        onClick = { action(KeyboardAction.SelectCopiedSentence(index)) })
                    Text(sentence, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
            }
            s.copiedError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        }
        Button(onClick = { action(KeyboardAction.ConfirmCopiedSentence) },
            enabled = s.copiedSentences.isNotEmpty() && !s.detectingCopied, modifier = Modifier.fillMaxWidth()) {
            Text(uiText(if (s.detectingCopied) "正在本地识别…" else "识别这句并设置回复语言"))
        }
    }
}
