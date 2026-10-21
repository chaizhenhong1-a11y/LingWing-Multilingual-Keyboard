package com.lazytalk.keyboard.feature.keyboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.lazytalk.keyboard.feature.keyboard.presentation.*

/** Entirely bundled emoji; insertion uses the same safe host/draft path as ordinary keys. */
@Composable internal fun EmojiKeyboard(state: KeyboardUiState, action: (KeyboardAction) -> Unit, height: Dp, onSwitch: (() -> Unit)?) {
    listOf(
        listOf("😀", "😂", "🥰", "😍", "😊", "😎", "😭", "😅"),
        listOf("👍", "🙏", "👏", "🎉", "❤️", "🔥", "✨", "💯"),
        listOf("👋", "🤔", "😴", "😡", "🥳", "☕", "🍜", "🌍")
    ).forEach { row -> Row(Modifier.fillMaxWidth()) {
        row.forEach { emoji -> KeyButton(emoji, Modifier.weight(1f), height = height) { action(KeyboardAction.Key(emoji)) } }
    } }
    Row(Modifier.fillMaxWidth()) {
        KeyButton(state.typingLayout.name, Modifier.weight(1.5f), height = height, kind = KeyKind.FUNCTION) { action(KeyboardAction.Page(KeyPage.LETTERS)) }
        KeyButton("Space", Modifier.weight(3f), height = height, kind = KeyKind.SPACE, onLongClick = onSwitch) { action(KeyboardAction.Key(" ")) }
        KeyButton("⌫", Modifier.weight(1f), height = height, kind = KeyKind.FUNCTION, repeat = true) { action(KeyboardAction.Backspace) }
        KeyButton(state.enterLabel, Modifier.weight(1.5f), height = height, kind = KeyKind.ACTION) { action(KeyboardAction.Enter) }
    }
}
