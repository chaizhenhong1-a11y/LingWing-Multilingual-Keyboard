package com.lazytalk.keyboard.feature.keyboard.ime
import android.text.InputType.*
import android.view.inputmethod.EditorInfo
import org.junit.Assert.*
import org.junit.Test
class SecureFieldDetectorTest {
    @Test fun protectsPasswordsPinsAndNoLearningEditors() {
        listOf(TYPE_TEXT_VARIATION_PASSWORD, TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, TYPE_TEXT_VARIATION_WEB_PASSWORD).forEach {
            assertTrue(SecureFieldDetector.isSecure(TYPE_CLASS_TEXT or it))
        }
        assertTrue(SecureFieldDetector.isSecure(TYPE_CLASS_NUMBER or TYPE_NUMBER_VARIATION_PASSWORD))
        assertTrue(SecureFieldDetector.isSecure(TYPE_CLASS_NUMBER))
        assertTrue(SecureFieldDetector.isSecure(TYPE_CLASS_PHONE))
        assertTrue(SecureFieldDetector.isSecure(TYPE_NULL))
        assertTrue(SecureFieldDetector.isSecure(TYPE_CLASS_TEXT, EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING))
        assertFalse(SecureFieldDetector.isSecure(TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_EMAIL_ADDRESS))
        assertFalse(SecureFieldDetector.isSecure(TYPE_CLASS_TEXT or TYPE_TEXT_FLAG_MULTI_LINE))
    }
}
