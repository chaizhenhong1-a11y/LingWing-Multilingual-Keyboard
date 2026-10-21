package com.lazytalk.keyboard.translation.mock

import com.lazytalk.keyboard.core.common.*
import com.lazytalk.keyboard.core.model.*
import com.lazytalk.keyboard.translation.TranslationService
import kotlinx.coroutines.delay

/** Deliberately bounded fixtures: never present an invented translation as AI output. */
class MockTranslationService : TranslationService {
    private data class Fixture(val source: String, val forms: Map<String, List<String>>)
    private val fixtures = listOf(
        Fixture("zh", mapOf("zh" to listOf("我等下才过去，你先吃。"), "en" to listOf("I'll come over later. You eat first.", "I'll head over later, so you can eat first.", "I'll come by later. Go ahead and eat first."), "ms" to listOf("Saya datang nanti. Awak makan dulu."))),
        Fixture("en", mapOf("en" to listOf("I will call you when I arrive."), "ms" to listOf("Saya akan telefon awak apabila saya sampai.", "Nanti bila saya sampai, saya telefon awak."), "zh" to listOf("我到了就打电话给你。"))),
        Fixture("en", mapOf("en" to listOf("Where are you now?"), "zh" to listOf("你现在在哪里？", "你现在在哪儿？"), "ms" to listOf("Awak dekat mana sekarang?", "kat mana sekarang"))),
        Fixture("zh", mapOf("zh" to listOf("等下我才过去"), "en" to listOf("I'll come over later.", "I'll head over a bit later.", "I'll go over there later."))),
        Fixture("zh", mapOf("zh" to listOf("你吃了吗？"), "ms" to listOf("Awak dah makan?"), "en" to listOf("Have you eaten yet?"))),
        Fixture("iban", mapOf("iban" to listOf("Nama berita?"), "en" to listOf("How are you?"), "ms" to listOf("Apa khabar?")))
    )
    private fun normalize(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
    override suspend fun translate(request: TranslationRequest): TranslationResult {
        delay(250)
        val fixture = fixtures.firstOrNull { f -> f.forms.values.flatten().any { normalize(it) == normalize(request.text) } }
            ?: throw AppException(AppError.UNSUPPORTED_LANGUAGE)
        val detected = fixture.forms.entries.firstOrNull { e -> e.value.any { normalize(it) == normalize(request.text) } }?.key ?: fixture.source
        if (request.sourceLanguage != "auto" && request.sourceLanguage != detected) throw AppException(AppError.UNSUPPORTED_LANGUAGE)
        val candidates = fixture.forms[request.targetLanguage] ?: throw AppException(AppError.UNSUPPORTED_LANGUAGE)
        // All four tones are transmitted in Remote; fixtures intentionally do not pretend to rewrite tone.
        return TranslationResult(detected, candidates.take(3))
    }
}
