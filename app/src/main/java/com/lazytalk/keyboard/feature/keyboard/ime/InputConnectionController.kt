package com.lazytalk.keyboard.feature.keyboard.ime
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.ExtractedTextRequest
import com.lazytalk.keyboard.feature.keyboard.presentation.KeyboardEffect

class InputConnectionController(private val connection: () -> InputConnection?, private val editor: () -> EditorInfo?) {
    private data class Snapshot(val text: String, val offset: Int, val start: Int, val end: Int)
    private data class LiveRange(val expected: Snapshot, val start: Int, val end: Int)
    private var liveRange: LiveRange? = null
    private fun snapshot(ic: InputConnection): Snapshot? {
        val value = ic.getExtractedText(ExtractedTextRequest().apply { hintMaxChars = 20000 }, 0) ?: return null
        val text = value.text?.toString() ?: return null
        if (text.length > 20000 || value.selectionStart !in 0..text.length || value.selectionEnd !in 0..text.length) return null
        return Snapshot(text, value.startOffset, value.selectionStart, value.selectionEnd)
    }
    fun resetLiveTranslation() { liveRange = null }
    fun apply(effect: KeyboardEffect): Boolean {
        if (effect == KeyboardEffect.EndLiveTranslation) { resetLiveTranslation(); return true }
        val ic = connection() ?: return false
        when (effect) {
            KeyboardEffect.BeginLiveTranslation -> {
                resetLiveTranslation()
                val current = snapshot(ic) ?: return false
                if (current.start != current.end) return false
                liveRange = LiveRange(current, current.start, current.end)
            }
            is KeyboardEffect.UpdateLiveTranslation -> {
                val range = liveRange ?: return false
                val current = snapshot(ic) ?: return false
                if (current != range.expected) { resetLiveTranslation(); return false }
                if (range.start == range.end && effect.text.isEmpty()) return true
                val updated = current.text.replaceRange(range.start, range.end, effect.text)
                val caret = range.start + effect.text.length
                ic.beginBatchEdit()
                try {
                    if (range.start != range.end && !ic.setSelection(current.offset + range.start, current.offset + range.end)) return false
                    if (!ic.commitText(effect.text, 1)) return false
                } finally { ic.endBatchEdit() }
                val after = snapshot(ic) ?: return false
                val expected = Snapshot(updated, current.offset, caret, caret)
                if (after != expected) { resetLiveTranslation(); return false }
                liveRange = LiveRange(expected, range.start, caret)
            }
            KeyboardEffect.EndLiveTranslation -> Unit
            is KeyboardEffect.Composing -> { if(!ic.setComposingText(effect.text, 1)) return false }
            is KeyboardEffect.Commit -> ic.commitText(effect.text, 1)
            KeyboardEffect.Delete -> {
                if (!ic.getSelectedText(0).isNullOrEmpty()) ic.commitText("", 1)
                else if (!ic.deleteSurroundingTextInCodePoints(1, 0)) {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                }
            }
            KeyboardEffect.Enter -> {
                val options = editor()?.imeOptions ?: 0
                val action = options and EditorInfo.IME_MASK_ACTION
                if (options and EditorInfo.IME_FLAG_NO_ENTER_ACTION == 0 && action !in setOf(EditorInfo.IME_ACTION_NONE, EditorInfo.IME_ACTION_UNSPECIFIED)) ic.performEditorAction(action)
                else ic.commitText("\n", 1)
            }
        }
        return true
    }
    fun selectedText(): String = connection()?.getSelectedText(0)?.take(4000)?.toString().orEmpty()
    companion object {
        fun actionLabel(info: EditorInfo): String = if (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) "Enter" else when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_DONE -> "Done"
            EditorInfo.IME_ACTION_NEXT -> "Next"
            EditorInfo.IME_ACTION_PREVIOUS -> "Prev"
            EditorInfo.IME_ACTION_SEARCH -> "Search"
            EditorInfo.IME_ACTION_SEND -> "Send"
            EditorInfo.IME_ACTION_GO -> "Go"
            else -> "Enter"
        }
    }
}
