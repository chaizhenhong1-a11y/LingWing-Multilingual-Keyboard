package com.lazytalk.keyboard.core.data
import com.lazytalk.keyboard.core.data.translation.TranslationRepositoryImpl
import com.lazytalk.keyboard.core.common.*
import com.lazytalk.keyboard.core.model.*
import com.lazytalk.keyboard.testutil.FakeSettings
import com.lazytalk.keyboard.translation.TranslationService
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
class TranslationRepositoryTest {
    private val input = TranslationRequest("hi", "auto", "ms", Tone.NATURAL)
    @Test fun selectsProviderFromSettings() = runTest {
        val settings = FakeSettings()
        val repository = TranslationRepositoryImpl(settings) { config -> object : TranslationService {
            override suspend fun translate(request: TranslationRequest) = TranslationResult("en", listOf(config.mode.name))
        } }
        assertEquals("MOCK", repository.translate(input).candidates.single())
        settings.update { it.copy(mode = TranslationMode.REMOTE) }
        assertEquals("REMOTE", repository.translate(input).candidates.single())
    }
    @Test fun mapsUnexpectedFailuresWithoutLeakingDetails() = runTest {
        val repository = TranslationRepositoryImpl(FakeSettings()) { object : TranslationService {
            override suspend fun translate(request: TranslationRequest): TranslationResult = error("private input")
        } }
        try { repository.translate(input); fail() } catch (e: AppException) { assertEquals(AppError.UNKNOWN, e.error); assertFalse(e.message.orEmpty().contains("private")) }
    }
    @Test fun propagatesCancellation() = runTest {
        val repository = TranslationRepositoryImpl(FakeSettings()) { object : TranslationService {
            override suspend fun translate(request: TranslationRequest): TranslationResult = throw CancellationException()
        } }
        try { repository.translate(input); fail() } catch (_: CancellationException) { }
    }
}
