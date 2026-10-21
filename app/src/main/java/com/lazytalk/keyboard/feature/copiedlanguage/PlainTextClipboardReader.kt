package com.lazytalk.keyboard.feature.copiedlanguage

import android.content.ClipboardManager

/** Explicit one-shot read. Never registers a clipboard listener or dereferences clip URIs. */
class PlainTextClipboardReader(private val clipboard: ClipboardManager) {
    fun read(): String? {
        val clip = clipboard.primaryClip ?: return null
        if (clip.description.extras?.getBoolean("android.content.extra.IS_SENSITIVE") == true) return null
        return clip.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.take(4001)?.toString()
    }
}
