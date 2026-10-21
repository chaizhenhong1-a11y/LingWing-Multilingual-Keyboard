package com.lazytalk.keyboard.feature.keyboard.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp

enum class KeyKind { LETTER, FUNCTION, SPACE, ACTION }

@Composable fun KeyButton(label: String, modifier: Modifier = Modifier, repeat: Boolean = false, height: Dp = 48.dp, kind: KeyKind = KeyKind.LETTER, hint: String? = null, longClickLabel: String = "Switch input method", onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val feedback by rememberUpdatedState(LocalKeyFeedback.current)
    val callback by rememberUpdatedState(onClick)
    val longCallback by rememberUpdatedState(onLongClick)
    Box(modifier.height(height + 2.dp).testTag("keyTarget:$label").semantics {
        contentDescription = when(label) { "⌫" -> "Backspace"; "⇧" -> "Shift"; "☺" -> "Emoji"; else -> label }
        role = Role.Button
        onClick { feedback(); callback(); true }
        if (onLongClick != null) onLongClick(longClickLabel) { feedback(); longCallback?.invoke(); true }
    }.pointerInput(repeat, onLongClick != null) {
        detectTapGestures(onTap = { if (!repeat && longCallback != null) callback() },
            onLongPress = if (onLongClick != null) ({ _: androidx.compose.ui.geometry.Offset -> longCallback?.invoke() }) else null,
            onPress = {
            feedback()
            if (repeat || longCallback == null) callback()
            if (repeat) coroutineScope {
                val job = launch { delay(400); while (isActive) { callback(); delay(65) } }
                try { tryAwaitRelease() } finally { job.cancel() }
            } else tryAwaitRelease()
        })
    }) {
      Surface(Modifier.fillMaxSize().padding(horizontal=2.5.dp,vertical=3.dp).testTag("keyCap:$label"), shape = RoundedCornerShape(if (kind == KeyKind.ACTION || kind == KeyKind.SPACE) 22.dp else 8.dp),
        color = when (kind) {
            KeyKind.ACTION -> MaterialTheme.colorScheme.primaryContainer
            KeyKind.FUNCTION -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        }, contentColor = if (kind == KeyKind.ACTION) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            hint?.let { Text(it, Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(top = 2.dp, end = 4.dp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (label == "☺") EmojiKeyIcon()
            else Text(label, fontSize = if (kind == KeyKind.LETTER || kind == KeyKind.SPACE) 20.sp else 15.sp, maxLines = 1)
        }
    }
    }
}
