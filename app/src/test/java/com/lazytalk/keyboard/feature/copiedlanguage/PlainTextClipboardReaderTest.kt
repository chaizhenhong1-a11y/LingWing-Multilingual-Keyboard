package com.lazytalk.keyboard.feature.copiedlanguage

import android.content.*
import android.net.Uri
import android.os.PersistableBundle
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PlainTextClipboardReaderTest {
    @Test fun readsLatestClipOnlyWhenExplicitlyCalledAndDoesNotConsumeIt() {
        val manager = ApplicationProvider.getApplicationContext<Context>().getSystemService(ClipboardManager::class.java)
        manager.setPrimaryClip(ClipData.newPlainText("sample", "First sentence."))
        val reader = PlainTextClipboardReader(manager)
        manager.setPrimaryClip(ClipData.newPlainText("sample", "Second sentence."))
        assertEquals("Second sentence.", reader.read())
        assertEquals("Second sentence.", manager.primaryClip!!.getItemAt(0).text.toString())
    }
    @Test fun ignoresSensitiveClipsAndNeverCoercesUriIntoText() {
        val manager = ApplicationProvider.getApplicationContext<Context>().getSystemService(ClipboardManager::class.java)
        val secret = ClipData.newPlainText("secret", "password sample")
        secret.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
        manager.setPrimaryClip(secret)
        assertNull(PlainTextClipboardReader(manager).read())
        manager.setPrimaryClip(ClipData.newRawUri("uri", Uri.parse("content://private.example/item")))
        assertNull(PlainTextClipboardReader(manager).read())
    }
}
