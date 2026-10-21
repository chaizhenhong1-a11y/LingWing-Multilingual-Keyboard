package com.lazytalk.keyboard.core.data
import com.lazytalk.keyboard.core.data.settings.SettingsCodec
import com.lazytalk.keyboard.core.model.*
import org.junit.Assert.*
import org.junit.Test
class SettingsCodecTest {
    @Test fun unknownPersistedSettingsHaveSafeDefaults() {
        assertEquals(KeyVibrationStrength.STANDARD,SettingsCodec.vibrationStrength("future"))
        assertEquals(KeyVibrationStrength.STRONG,SettingsCodec.vibrationStrength("STRONG"))
        assertEquals(Tone.NATURAL, SettingsCodec.tone("future"))
        assertEquals(TranslationMode.MOCK, SettingsCodec.mode(null))
        assertEquals(Tone.POLITE, SettingsCodec.tone("POLITE"))
        assertEquals(listOf("en", "ms"), SettingsCodec.recent("en,ms,en,,"))
    }
}
