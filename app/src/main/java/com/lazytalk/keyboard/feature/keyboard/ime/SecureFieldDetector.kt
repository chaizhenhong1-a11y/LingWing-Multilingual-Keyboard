package com.lazytalk.keyboard.feature.keyboard.ime

import android.text.InputType
import android.view.inputmethod.EditorInfo

object SecureFieldDetector {
    fun isSecure(inputType: Int, imeOptions: Int = 0): Boolean {
        val cls = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return inputType == InputType.TYPE_NULL ||
            (imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0) ||
            (cls == InputType.TYPE_CLASS_TEXT && variation in setOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
            cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE
        // Android has no universal credit-card/PIN flag; number/phone fields are conservative secure defaults.
    }
}
