package com.lazytalk.keyboard.feature.keyboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lazytalk.keyboard.feature.keyboard.presentation.*

@Composable fun KeyboardRows(state: KeyboardUiState, action: (KeyboardAction) -> Unit, keyHeight: Dp = 48.dp, onSwitch: (() -> Unit)? = null) {
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
        KeyboardRowContent(state, action, keyHeight, onSwitch)
    }
}
@Composable private fun KeyboardRowContent(state: KeyboardUiState, action: (KeyboardAction) -> Unit, keyHeight: Dp = 48.dp, onSwitch: (() -> Unit)? = null) {
    if (state.page == KeyPage.EMOJI) {
        EmojiKeyboard(state, action, keyHeight, onSwitch)
        return
    }
    val rows = when (state.page) {
        KeyPage.LETTERS -> if (state.shift == ShiftState.OFF) state.typingLayout.tokenRows else state.typingLayout.shiftedTokenRows
        KeyPage.NUMBERS -> listOf("1234567890", "@#£_&-+()/", "*\"':;!?,.").map { row -> row.map { it.toString() } }
        KeyPage.EMOJI -> emptyList()
        KeyPage.SYMBOLS -> listOf("~`|•√π÷×¶∆", "€¥$¢^°={}\\", "%©®™✓[]<>").map { row -> row.map { it.toString() } }
    }
    rows.forEachIndexed { index, row ->
        Row(Modifier.fillMaxWidth()) {
            val staggered = state.page == KeyPage.LETTERS && state.typingLayout.stagger && index == 1
            if (staggered) Spacer(Modifier.weight(0.5f))
            if (index == rows.lastIndex) KeyButton(if (state.page == KeyPage.LETTERS) when (state.shift) {
                ShiftState.OFF -> "⇧"; ShiftState.ON -> "Shift"; ShiftState.CAPS -> "CAPS"
            } else "#+=", Modifier.weight(1.5f), height = keyHeight, kind = KeyKind.FUNCTION) {
                action(if (state.page == KeyPage.LETTERS) KeyboardAction.Shift else KeyboardAction.Page(if (state.page == KeyPage.NUMBERS) KeyPage.SYMBOLS else KeyPage.NUMBERS))
            }
            row.forEachIndexed { position, key ->
                val digit = if (state.page == KeyPage.LETTERS && index == 0 && position < 10) "1234567890"[position].toString() else null
                CharacterKey(key, digit, state.typingLayout.popups[key.lowercase()].orEmpty(),
                    state.shift != ShiftState.OFF, Modifier.weight(1f), keyHeight, action)
            }
            if (staggered) Spacer(Modifier.weight(0.5f))
            if (index == rows.lastIndex) KeyButton("⌫", Modifier.weight(1.5f), repeat = true, height = keyHeight, kind = KeyKind.FUNCTION) { action(KeyboardAction.Backspace) }
        }
    }
    Row(Modifier.fillMaxWidth()) {
        KeyButton(if (state.page == KeyPage.LETTERS) "?123" else state.typingLayout.name, Modifier.weight(1.3f), height = keyHeight, kind = KeyKind.FUNCTION) {
            action(KeyboardAction.Page(if (state.page == KeyPage.LETTERS) KeyPage.NUMBERS else KeyPage.LETTERS))
        }
        KeyButton(",", Modifier.weight(0.8f), height = keyHeight) { action(KeyboardAction.Key(",")) }
        KeyButton("☺", Modifier.weight(0.8f), height = keyHeight, kind = KeyKind.FUNCTION) { action(KeyboardAction.Page(KeyPage.EMOJI)) }
        KeyButton("Space", Modifier.weight(3.5f), height = keyHeight, kind = KeyKind.SPACE, onLongClick = onSwitch) { action(KeyboardAction.Key(" ")) }
        KeyButton(".", Modifier.weight(0.8f), height = keyHeight) { action(KeyboardAction.Key(".")) }
        KeyButton(state.enterLabel, Modifier.weight(1.5f), height = keyHeight, kind = KeyKind.ACTION) { action(KeyboardAction.Enter) }
    }
}
