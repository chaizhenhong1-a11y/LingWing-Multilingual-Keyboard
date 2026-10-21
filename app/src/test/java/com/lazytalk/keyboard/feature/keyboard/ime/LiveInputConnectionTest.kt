package com.lazytalk.keyboard.feature.keyboard.ime

import android.app.Activity
import android.view.inputmethod.*
import android.widget.EditText
import com.lazytalk.keyboard.feature.keyboard.presentation.KeyboardEffect
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk = [28])
class LiveInputConnectionTest {
    private lateinit var field: EditText
    private lateinit var input: InputConnectionController
    private var sent = 0
    @Before fun setup() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        field = EditText(activity); activity.setContentView(field)
        field.setText("prefix suffix"); field.setSelection(7)
        val info = EditorInfo()
        val connection = object : InputConnectionWrapper(field.onCreateInputConnection(info), true) {
            override fun performEditorAction(actionCode: Int): Boolean { sent++; return true }
        }
        input = InputConnectionController({ connection }, { info })
    }
    @Test fun composingUpdatesReplaceTheSameSpanAndCommitDoesNotDuplicate() {
        input.apply(KeyboardEffect.Composing("ni"))
        input.apply(KeyboardEffect.Composing("nihao"))
        assertEquals("prefix nihaosuffix",field.text.toString())
        input.apply(KeyboardEffect.Commit("你好"))
        assertEquals("prefix 你好suffix",field.text.toString())
        assertEquals(0,sent)
    }
    @Test fun replacesOnlyItsOwnOutputAndClearingDraftPreservesOtherText() {
        assertTrue(input.apply(KeyboardEffect.BeginLiveTranslation))
        assertTrue(input.apply(KeyboardEffect.UpdateLiveTranslation("one")))
        assertEquals("prefix onesuffix", field.text.toString())
        assertTrue(input.apply(KeyboardEffect.UpdateLiveTranslation("two longer")))
        assertEquals("prefix two longersuffix", field.text.toString())
        assertTrue(input.apply(KeyboardEffect.UpdateLiveTranslation("")))
        assertEquals("prefix suffix", field.text.toString()); assertEquals(0, sent)
    }
    @Test fun refusesToOverwriteAfterExternalEditCursorMoveOrSend() {
        assertTrue(input.apply(KeyboardEffect.BeginLiveTranslation))
        assertTrue(input.apply(KeyboardEffect.UpdateLiveTranslation("one")))
        field.setSelection(0)
        assertFalse(input.apply(KeyboardEffect.UpdateLiveTranslation("unsafe")))
        assertEquals("prefix onesuffix", field.text.toString())
        assertTrue(input.apply(KeyboardEffect.BeginLiveTranslation))
        field.setText("a different draft"); field.setSelection(field.length())
        assertFalse(input.apply(KeyboardEffect.UpdateLiveTranslation("unsafe")))
        assertEquals("a different draft", field.text.toString())
        assertTrue(input.apply(KeyboardEffect.BeginLiveTranslation))
        field.setText("")
        assertFalse(input.apply(KeyboardEffect.UpdateLiveTranslation("must not reappear")))
        assertEquals("", field.text.toString()); assertEquals(0, sent)
    }
    @Test fun selectedHostTextAndUnsupportedEditorsAreNotOverwritten() {
        field.setSelection(0, 6)
        assertFalse(input.apply(KeyboardEffect.BeginLiveTranslation))
        assertFalse(input.apply(KeyboardEffect.UpdateLiveTranslation("unsafe")))
        assertEquals("prefix suffix", field.text.toString())
        val unavailable = InputConnectionController({ BaseInputConnection(field, false) }, { EditorInfo() })
        assertFalse(unavailable.apply(KeyboardEffect.BeginLiveTranslation))
    }
}
