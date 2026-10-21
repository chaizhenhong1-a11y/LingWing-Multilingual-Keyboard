package com.lazytalk.keyboard.translation
import com.lazytalk.keyboard.core.common.AppException
import com.lazytalk.keyboard.core.model.*
import com.lazytalk.keyboard.translation.mock.MockTranslationService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
class MockTranslationServiceTest {
    @Test fun acceptancePhrasesAndDetection() = runTest {
        val mock = MockTranslationService()
        val chinese = mock.translate(TranslationRequest("我等下才过去，你先吃。", "auto", "en", Tone.NATURAL))
        assertEquals("zh", chinese.detectedLanguage)
        assertEquals(3, chinese.candidates.size)
        assertEquals("I'll come over later. You eat first.", chinese.candidates.first())
        val malay = mock.translate(TranslationRequest("I will call you when I arrive.", "auto", "ms", Tone.POLITE))
        assertEquals("en", malay.detectedLanguage)
        assertTrue(malay.candidates.first().contains("telefon"))
        assertEquals("ms", mock.translate(TranslationRequest("kat mana sekarang", "auto", "zh", Tone.NATURAL)).detectedLanguage)
        assertEquals("iban", mock.translate(TranslationRequest("Nama berita?", "auto", "en", Tone.NATURAL)).detectedLanguage)
    }
    @Test fun unknownPhraseFailsHonestly() = runTest {
        try { MockTranslationService().translate(TranslationRequest("unknown", "auto", "en", Tone.NATURAL)); fail() } catch (_: AppException) { }
    }
}
