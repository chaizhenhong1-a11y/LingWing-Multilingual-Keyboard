package com.lazytalk.keyboard.feature.keyboard.presentation

import com.lazytalk.keyboard.core.data.TranslationRepository
import com.lazytalk.keyboard.core.model.*
import com.lazytalk.keyboard.feature.keyboard.domain.TranslateTextUseCase
import com.lazytalk.keyboard.testutil.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class LiveTranslationTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { Dispatchers.resetMain() }
    private val calls = mutableListOf<TranslationRequest>()
    private val repository = object : TranslationRepository {
        override suspend fun translate(request: TranslationRequest): TranslationResult {
            calls += request; delay(1000)
            return TranslationResult("en", listOf(request.text + ":" + request.targetLanguage))
        }
    }
    private fun model(mode: TranslationMode = TranslationMode.REMOTE) = KeyboardViewModel(
        FakeSettings(UserSettings(mode = mode)), FakeLanguages(), TranslateTextUseCase(repository))

    @Test fun debounceCancelsObsoleteRequestAndOnlyUpdatesWithLatestTranslation() = runTest(dispatcher) {
        val vm = model(); val effects = mutableListOf<KeyboardEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.collect { effects += it } }
        runCurrent(); vm.beginSession(false, false, "Send"); vm.dispatch(KeyboardAction.ToggleDraft)
        vm.dispatch(KeyboardAction.Key("hello")); advanceTimeBy(499); runCurrent(); assertTrue(calls.isEmpty())
        advanceTimeBy(1); runCurrent(); assertEquals("hello", calls.single().text)
        vm.dispatch(KeyboardAction.Key(" world")); advanceTimeBy(500); runCurrent()
        assertEquals(2, calls.size); advanceTimeBy(1000); runCurrent()
        assertEquals(listOf(KeyboardEffect.UpdateLiveTranslation("hello world:en")), effects.filterIsInstance<KeyboardEffect.UpdateLiveTranslation>())
        assertEquals(1, effects.count { it == KeyboardEffect.BeginLiveTranslation })
        assertTrue(vm.state.value.candidates.isEmpty()); assertTrue(vm.state.value.draftMode)
        vm.dispatch(KeyboardAction.Clear); advanceTimeBy(500); runCurrent()
        assertEquals(KeyboardEffect.UpdateLiveTranslation(""), effects.last()); assertEquals(2, calls.size)
    }
    @Test fun targetPickerPausesAndResumesWithoutStartingANewInsertionRange() = runTest(dispatcher) {
        val vm = model(); val effects = mutableListOf<KeyboardEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.collect { effects += it } }
        runCurrent(); vm.beginSession(false, false, "Send"); vm.importDraft("hello"); advanceTimeBy(1500); runCurrent()
        vm.dispatch(KeyboardAction.OpenPicker(false)); advanceTimeBy(2000); runCurrent(); assertEquals(1, calls.size)
        vm.dispatch(KeyboardAction.PickLanguage("ms")); advanceTimeBy(1500); runCurrent()
        assertEquals("ms", calls.last().targetLanguage)
        assertEquals(1, effects.count { it == KeyboardEffect.BeginLiveTranslation })
        assertFalse(effects.contains(KeyboardEffect.EndLiveTranslation))
    }
    @Test fun changedHostPausesUntilExplicitlyReenabledAndSecureSessionCancels() = runTest(dispatcher) {
        val vm = model(); runCurrent(); vm.beginSession(false, false, "Send"); vm.importDraft("hello")
        advanceTimeBy(500); runCurrent(); assertEquals(1, calls.size)
        vm.liveInsertionFailed(); vm.dispatch(KeyboardAction.Key(" more")); advanceTimeBy(3000); runCurrent()
        assertEquals(1, calls.size); assertTrue(vm.state.value.liveBlocked)
        vm.dispatch(KeyboardAction.ToggleLiveTranslation); vm.dispatch(KeyboardAction.ToggleLiveTranslation)
        advanceTimeBy(500); runCurrent(); assertEquals(2, calls.size)
        vm.beginSession(true, false, "Done"); advanceTimeBy(3000); runCurrent()
        assertTrue(vm.state.value.draft.isEmpty()); assertTrue(vm.state.value.candidates.isEmpty())
    }
    @Test fun mockModeDoesNotMakeAutomaticRequests() = runTest(dispatcher) {
        val vm = model(TranslationMode.MOCK); runCurrent(); vm.beginSession(false, false, "Send")
        vm.dispatch(KeyboardAction.ToggleDraft); vm.dispatch(KeyboardAction.Key("hello"))
        advanceTimeBy(3000); runCurrent(); assertTrue(calls.isEmpty())
    }
    @Test fun queuedOldTranslationIsDiscardedBeforeDelivery() = runTest(dispatcher) {
        val vm = model(); runCurrent(); vm.beginSession(false, false, "Send"); vm.importDraft("old")
        advanceTimeBy(1500); runCurrent()
        vm.importDraft("new"); advanceTimeBy(1500); runCurrent()
        assertEquals(KeyboardEffect.UpdateLiveTranslation("new:en"),
            vm.effects.filterIsInstance<KeyboardEffect.UpdateLiveTranslation>().first())
    }
}
