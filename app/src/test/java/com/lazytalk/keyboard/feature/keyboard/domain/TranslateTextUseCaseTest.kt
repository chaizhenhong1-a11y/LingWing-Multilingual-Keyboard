package com.lazytalk.keyboard.feature.keyboard.domain
import com.lazytalk.keyboard.core.common.*
import com.lazytalk.keyboard.core.data.TranslationRepository
import com.lazytalk.keyboard.core.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
class TranslateTextUseCaseTest {
    @Test fun secureOrBlankInputsNeverReachRepository() = runTest {
        var calls = 0
        val useCase = TranslateTextUseCase(object : TranslationRepository {
            override suspend fun translate(request: TranslationRequest): TranslationResult { calls++; return TranslationResult("en", listOf("你好")) }
        })
        listOf("secret" to true, " " to false, "x".repeat(4001) to false).forEach { (text, secure) ->
            try { useCase(TranslationRequest(text, "auto", "zh", Tone.NATURAL), secure); fail() } catch (_: AppException) { }
        }
        assertEquals(0, calls)
        assertEquals("en", useCase(TranslationRequest("hello", "auto", "zh", Tone.NATURAL), false).detectedLanguage)
    }
}
