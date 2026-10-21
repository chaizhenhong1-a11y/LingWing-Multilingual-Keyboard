package com.lazytalk.keyboard.feature.keyboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.lazytalk.keyboard.feature.keyboard.presentation.*

@Composable internal fun CharacterKey(text: String, digit: String?, alternatives: List<String>, shifted: Boolean,
    modifier: Modifier, height: Dp, action: (KeyboardAction) -> Unit) {
    var expanded by remember(text) { mutableStateOf(false) }
    val choices = (listOfNotNull(digit) + alternatives.map { if (shifted) it.uppercase() else it }).distinct()
    Box(modifier) {
        val mark = text.codePointAt(0).let { Character.getType(it) in listOf(Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt()) }
        KeyButton(if (mark) "◌$text" else text, Modifier.fillMaxWidth(), height = height, hint = digit,
            longClickLabel = if (alternatives.isEmpty()) "输入 ${digit.orEmpty()}" else "更多字符",
            onLongClick = if (choices.isEmpty()) null else ({
                if (alternatives.isEmpty() && digit != null) action(KeyboardAction.Key(digit)) else expanded = true
            })) { action(KeyboardAction.Key(text)) }
        DropdownMenu(expanded, { expanded = false }) {
            choices.forEach { value -> DropdownMenuItem(text = { Text(value) }, onClick = {
                expanded = false; action(KeyboardAction.Key(value))
            }) }
        }
    }
}
