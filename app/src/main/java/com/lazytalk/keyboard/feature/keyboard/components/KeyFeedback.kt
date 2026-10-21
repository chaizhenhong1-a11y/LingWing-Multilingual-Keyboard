package com.lazytalk.keyboard.feature.keyboard.components

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext

internal val LocalKeyFeedback = staticCompositionLocalOf<() -> Unit> { {} }

/** Explicit app-controlled key pulse, with platform feedback as a no-motor fallback. */
@Composable internal fun KeyFeedback(enabled: Boolean, feedback: (() -> Unit)? = null, durationMs: Int = 30, content: @Composable () -> Unit) {
    val view=LocalView.current
    val context=LocalContext.current
    val motor=remember(context) {KeyVibration(context)}
    val platformFeedback: () -> Unit = { if(!motor.pulse(durationMs)) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); Unit }
    val latest by rememberUpdatedState(feedback ?: platformFeedback)
    val pulse: () -> Unit = if(enabled) ({latest()}) else ({})
    CompositionLocalProvider(LocalKeyFeedback provides pulse, content=content)
}
