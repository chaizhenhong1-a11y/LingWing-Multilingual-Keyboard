package com.lazytalk.keyboard.feature.keyboard.components
import android.content.Context
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class KeyVibrationTest {
    @Test fun explicitPulseReachesTheMotorEvenWhenSystemTouchFeedbackIsOff() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        android.provider.Settings.System.putInt(context.contentResolver,android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED,0)
        val vibrator=context.getSystemService(Vibrator::class.java)
        val shadow=Shadows.shadowOf(vibrator)
        shadow.setHasVibrator(true)
        assertTrue(KeyVibration(context).pulse())
        assertTrue(shadow.isVibrating)
        vibrator.cancel()
        assertFalse(shadow.isVibrating)
    }
    @Test fun absentMotorDoesNotCrashTyping() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val shadow=Shadows.shadowOf(context.getSystemService(Vibrator::class.java))
        shadow.setHasVibrator(false)
        assertFalse(KeyVibration(context).pulse())
        assertFalse(shadow.isVibrating)
    }
}
