package com.lazytalk.keyboard.feature.keyboard.components

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lazytalk.keyboard.feature.settings.presentation.KeyVibrationSetting
import com.lazytalk.keyboard.ui.LazyTalkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],qualifiers="en-rUS-w411dp-h891dp-port")
class KeyFeedbackTest {
    @get:Rule val compose=createComposeRule()
    @Test fun switchingOffAndOnUpdatesFeedbackForAnAlreadyMountedKey() {
        var enabled by mutableStateOf(true);var pulses=0;var typed=0
        compose.setContent {LazyTalkTheme {KeyFeedback(enabled,{pulses++}) {KeyButton("a") {typed++}}}}
        compose.onNodeWithText("a").performTouchInput {click()}
        assertEquals(1,pulses);assertEquals(1,typed)
        compose.runOnIdle {enabled=false}
        compose.onNodeWithText("a").performTouchInput {click()}
        assertEquals(1,pulses);assertEquals(2,typed)
        compose.runOnIdle {enabled=true}
        compose.onNodeWithText("a").performClick()
        assertEquals(2,pulses);assertEquals(3,typed)
    }
    @Test fun longPressAndRepeatedBackspacePulseOnlyOncePerPhysicalPress() {
        var pulses=0;var switched=0;var deleted=0
        var repeat by mutableStateOf(false)
        compose.setContent {LazyTalkTheme {KeyFeedback(true,{pulses++}) {
            if(repeat) KeyButton("Backspace",repeat=true) {deleted++}
            else KeyButton("Space",onLongClick={switched++}) {}
        }}}
        compose.onNodeWithText("Space").performTouchInput {longClick()}
        assertEquals(1,pulses);assertEquals(1,switched)
        compose.runOnIdle {repeat=true}
        compose.onNodeWithText("Backspace").performTouchInput {down(center);advanceEventTime(650);up()}
        assertEquals(2,pulses);assertTrue(deleted>=1)
    }
    @Test fun vibrationSettingSwitchCanBeToggledByItsWholeRow() {
        var enabled by mutableStateOf(true)
        compose.setContent {LazyTalkTheme {KeyVibrationSetting(enabled) {enabled=it}}}
        compose.onNode(isToggleable()).assertIsOn().performClick().assertIsOff()
        assertFalse(enabled)
    }
    @Test fun selectingStrongUpdatesTheSettingAndAllLevelsRemainAvailable() {
        var strength by mutableStateOf(com.lazytalk.keyboard.core.model.KeyVibrationStrength.STANDARD)
        compose.setContent {LazyTalkTheme {
            com.lazytalk.keyboard.feature.settings.presentation.KeyVibrationStrengthSetting(strength) {strength=it}
        }}
        compose.onNodeWithText("Strong").performClick().assertIsSelected()
        assertEquals(com.lazytalk.keyboard.core.model.KeyVibrationStrength.STRONG,strength)
        compose.onNodeWithText("Light").assertIsDisplayed()
        compose.onNodeWithText("Standard").assertIsDisplayed()
    }
}
