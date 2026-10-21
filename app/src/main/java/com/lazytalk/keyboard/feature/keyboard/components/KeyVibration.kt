package com.lazytalk.keyboard.feature.keyboard.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Explicit short key pulse; the app's setting is enforced by callers before invoking it. */
internal class KeyVibration(context: Context) {
    private val appContext=context.applicationContext
    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? by lazy {
        if(Build.VERSION.SDK_INT>=31) appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else appContext.getSystemService(Vibrator::class.java)
    }
    fun pulse(durationMs: Int = 30): Boolean = try {
        val motor=vibrator
        if(motor==null || !motor.hasVibrator()) false else {
            motor.vibrate(VibrationEffect.createOneShot(durationMs.coerceIn(10,50).toLong(),VibrationEffect.DEFAULT_AMPLITUDE))
            true
        }
    } catch(_: RuntimeException) { false }
}
