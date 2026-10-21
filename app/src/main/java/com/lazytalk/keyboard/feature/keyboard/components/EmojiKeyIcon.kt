package com.lazytalk.keyboard.feature.keyboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Monochrome vector-style icon; never rendered by the platform emoji font. */
@Composable internal fun EmojiKeyIcon() {
    val ink = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(24.dp)) {
        val u = size.width / 24f
        drawCircle(ink, 9f*u, style = Stroke(1.8f*u))
        drawCircle(ink, 1f*u, Offset(8.5f*u, 9.5f*u))
        drawCircle(ink, 1f*u, Offset(15.5f*u, 9.5f*u))
        drawArc(ink, 15f, 150f, false, Offset(7f*u, 8f*u), Size(10f*u, 9f*u), style = Stroke(1.8f*u))
    }
}
