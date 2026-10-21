package com.lazytalk.keyboard.feature.keyboard.presentation
import com.lazytalk.keyboard.core.data.TranslationRepository
import com.lazytalk.keyboard.core.model.*
import com.lazytalk.keyboard.feature.keyboard.domain.TranslateTextUseCase
import com.lazytalk.keyboard.testutil.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
@OptIn(ExperimentalCoroutinesApi::class)
class KeyboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private fun model(repository: TranslationRepository) = KeyboardViewModel(FakeSettings(), FakeLanguages(), TranslateTextUseCase(repository))
    private val repository = object : TranslationRepository {
        override suspend fun translate(request: TranslationRequest): TranslationResult { delay(100); return TranslationResult("en", listOf("你好")) }
    }
    @Test fun pinyinEnterCommitsRawLettersBeforeOrAfterCandidatesWithoutEditorAction() = runTest(dispatcher) {
        val suggestions=object : com.lazytalk.keyboard.feature.keyboard.domain.TypingSuggestions {
            override fun candidates(engine: String,input: String)=listOf("这",input)
        }
        val vm=KeyboardViewModel(FakeSettings(),FakeLanguages(),TranslateTextUseCase(repository),typing=suggestions)
        runCurrent();vm.dispatch(KeyboardAction.PickLayout("zh"));runCurrent()
        val effects=mutableListOf<KeyboardEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effects.collect { effects+=it } }
        repeat(2) { attempt ->
            "zh".forEach { vm.dispatch(KeyboardAction.Key(it.toString())) }
            if(attempt==1) vm.state.first { "这" in it.typingCandidates }
            vm.dispatch(KeyboardAction.Enter)
            runCurrent()
            assertEquals("",vm.state.value.composition)
        }
        assertEquals(listOf("zh","zh"),effects.filterIsInstance<KeyboardEffect.Commit>().map { it.text })
        assertFalse(effects.any { it==KeyboardEffect.Enter })
    }
    @Test fun pinyinEnterInTranslationDraftInsertsLettersAtCaret() = runTest(dispatcher) {
        val vm=model(repository)
        runCurrent();vm.importDraft("ab")
        vm.dispatch(KeyboardAction.EditDraft("ab",androidx.compose.ui.text.TextRange(1)))
        vm.dispatch(KeyboardAction.PickLayout("zh"));runCurrent()
        "zh".forEach { vm.dispatch(KeyboardAction.Key(it.toString())) }
        vm.dispatch(KeyboardAction.Enter)
        assertEquals("azhb",vm.state.value.draft)
        assertEquals("",vm.state.value.composition)
    }
    @Test fun fastSpaceWaitsForDictionaryInsteadOfDroppingTheCommitKey() = runTest(dispatcher) {
        val suggestions=object : com.lazytalk.keyboard.feature.keyboard.domain.TypingSuggestions {
            override fun candidates(engine: String,input: String)=listOf("你好",input)
        }
        val vm=KeyboardViewModel(FakeSettings(),FakeLanguages(),TranslateTextUseCase(repository),typing=suggestions)
        runCurrent();vm.dispatch(KeyboardAction.PickLayout("zh"));runCurrent()
        val committed=async { vm.effects.filterIsInstance<KeyboardEffect.Commit>().first() }
        "nihao ".forEach { vm.dispatch(KeyboardAction.Key(it.toString())) }
        assertEquals("你好",committed.await().text)
        assertEquals("",vm.state.value.composition)
    }
    @Test fun phoneticInputCommitsChosenChineseAndLeavesTranslationTargetIndependent() = runTest(dispatcher) {
        val suggestions=object : com.lazytalk.keyboard.feature.keyboard.domain.TypingSuggestions {
            override fun candidates(engine: String,input: String)=listOf("你好",input)
        }
        val vm=KeyboardViewModel(FakeSettings(),FakeLanguages(),TranslateTextUseCase(repository),typing=suggestions)
        runCurrent();val target=vm.state.value.target
        vm.dispatch(KeyboardAction.PickLayout("zh"));runCurrent()
        "nihao".forEach { vm.dispatch(KeyboardAction.Key(it.toString())) }
        vm.state.first { "你好" in it.typingCandidates }
        val committed=async { vm.effects.filterIsInstance<KeyboardEffect.Commit>().first() }
        vm.dispatch(KeyboardAction.TypingCandidate("你好"))
        assertEquals("你好",committed.await().text)
        assertEquals("",vm.state.value.composition)
        assertEquals(target,vm.state.value.target)
    }
    @Test fun draftCompositionIsInsertedAtCaretOnlyAfterCandidateSelectionAndClearsOnSession() = runTest(dispatcher) {
        val suggestions=object : com.lazytalk.keyboard.feature.keyboard.domain.TypingSuggestions {
            override fun candidates(engine: String,input: String)=listOf("你好",input)
        }
        val vm=KeyboardViewModel(FakeSettings(),FakeLanguages(),TranslateTextUseCase(repository),typing=suggestions)
        runCurrent();vm.importDraft("ab");vm.dispatch(KeyboardAction.EditDraft("ab",androidx.compose.ui.text.TextRange(1)))
        vm.dispatch(KeyboardAction.PickLayout("zh"));runCurrent()
        vm.dispatch(KeyboardAction.Key("n"))
        vm.state.first { "你好" in it.typingCandidates }
        assertEquals("ab",vm.state.value.draft)
        vm.dispatch(KeyboardAction.TypingCandidate("你好"))
        assertEquals("a你好b",vm.state.value.draft)
        vm.dispatch(KeyboardAction.Key("n"));vm.endSession()
        assertEquals("",vm.state.value.composition)
        assertTrue(vm.state.value.typingCandidates.isEmpty())
    }
    @Test fun typingLayoutPersistsAndCommitsNativeCharactersWithoutChangingTranslationTarget() = runTest(dispatcher) {
        val settings = FakeSettings()
        val vm = KeyboardViewModel(settings, FakeLanguages(), TranslateTextUseCase(repository))
        runCurrent()
        val target = vm.state.value.target
        for (layout in TypingLayouts.all.filter { it.engine == "direct" }) {
            vm.dispatch(KeyboardAction.OpenLayouts)
            vm.dispatch(KeyboardAction.PickLayout(layout.id)); runCurrent()
            assertEquals(layout, vm.state.value.typingLayout)
            assertEquals(target, vm.state.value.target)
            assertFalse(vm.state.value.layoutPicker)
            val effect = async { vm.effects.first() }
            vm.dispatch(KeyboardAction.Key(layout.rows[0].first().toString()))
            assertEquals(KeyboardEffect.Commit(layout.rows[0].first().toString()), effect.await())
        }
        val reopened = KeyboardViewModel(settings, FakeLanguages(), TranslateTextUseCase(repository))
        runCurrent()
        assertEquals(TypingLayouts.all.last { it.engine == "direct" }.id, reopened.state.value.typingLayout.id)
    }

    @Test fun copiedPreviewDoesNotIdentifyUntilConfirmedAndOnlySelectedSentenceIsUsed() = runTest(dispatcher) {
        val seen = mutableListOf<String>()
        var translated = 0
        val detector = object : com.lazytalk.keyboard.feature.copiedlanguage.SentenceLanguageDetector {
            override suspend fun identify(text: String): List<Pair<String, Float>> { seen += text; return listOf("ms" to .99f) }
        }
        val translation = object : TranslationRepository {
            override suspend fun translate(request: TranslationRequest): TranslationResult { translated++; error("Not expected") }
        }
        val vm = KeyboardViewModel(FakeSettings(), FakeLanguages(), TranslateTextUseCase(translation), detector)
        runCurrent(); vm.beginSession(false, false, "Send"); vm.importDraft("My reply draft")
        vm.prepareCopiedText("Where are you now?\nAwak dekat mana sekarang?")
        runCurrent(); assertTrue(seen.isEmpty()); assertEquals("en", vm.state.value.target)
        vm.dispatch(KeyboardAction.SelectCopiedSentence(1)); vm.dispatch(KeyboardAction.ConfirmCopiedSentence); advanceUntilIdle()
        assertEquals(listOf("Awak dekat mana sekarang?"), seen)
        assertEquals("ms", vm.state.value.target); assertEquals("My reply draft", vm.state.value.draft)
        assertTrue(vm.state.value.copiedPanelOpen); assertEquals(2, vm.state.value.copiedSentences.size)
        assertEquals(0, translated)
    }
    @Test fun pastingOneReferenceAutomaticallySetsReplyTargetThenTranslatesOnlyOwnDraft() = runTest(dispatcher) {
        val seen = mutableListOf<String>()
        val requests = mutableListOf<TranslationRequest>()
        val detector = object : com.lazytalk.keyboard.feature.copiedlanguage.SentenceLanguageDetector {
            override suspend fun identify(text: String): List<Pair<String, Float>> {
                seen += text; return listOf("ms" to .99f)
            }
        }
        val translation = object : TranslationRepository {
            override suspend fun translate(request: TranslationRequest): TranslationResult {
                requests += request; return TranslationResult("zh", listOf("Saya mahu makan."))
            }
        }
        val vm = KeyboardViewModel(FakeSettings(), FakeLanguages(), TranslateTextUseCase(translation), detector)
        runCurrent(); vm.beginSession(false, false, "Send")
        vm.pasteReplyReference("Awak dekat mana sekarang?"); advanceUntilIdle()
        assertEquals(listOf("Awak dekat mana sekarang?"), seen)
        assertEquals("ms", vm.state.value.target); assertEquals("auto", vm.state.value.source)
        assertTrue(vm.state.value.draftMode); assertEquals("", vm.state.value.draft)
        assertTrue(vm.state.value.copiedPanelOpen); assertTrue(requests.isEmpty())
        vm.dispatch(KeyboardAction.CloseCopiedSentence)
        vm.importDraft("我要吃饭"); vm.dispatch(KeyboardAction.Translate); advanceUntilIdle()
        assertEquals("我要吃饭", requests.single().text)
        assertEquals("ms", requests.single().targetLanguage)
    }
    @Test fun repeatedPastesStayOpenAndSelectingAnEarlierSentenceChangesTarget() = runTest(dispatcher) {
        val detector = object : com.lazytalk.keyboard.feature.copiedlanguage.SentenceLanguageDetector {
            override suspend fun identify(text: String): List<Pair<String, Float>> =
                listOf((if (text.startsWith("Awak")) "ms" else "en") to .99f)
        }
        val vm = KeyboardViewModel(FakeSettings(), FakeLanguages(), TranslateTextUseCase(repository), detector)
        runCurrent(); vm.beginSession(false, false, "Send"); vm.importDraft("My reply")
        vm.pasteReplyReference("Awak dekat mana sekarang?"); advanceUntilIdle()
        assertTrue(vm.state.value.copiedPanelOpen); assertEquals("ms", vm.state.value.target)
        vm.pasteReplyReference("Where are you now?"); advanceUntilIdle()
        assertTrue(vm.state.value.copiedPanelOpen); assertEquals("en", vm.state.value.target)
        assertEquals(2, vm.state.value.copiedSentences.size)
        vm.dispatch(KeyboardAction.SelectCopiedSentence(0)); advanceUntilIdle()
        assertEquals("ms", vm.state.value.target); assertTrue(vm.state.value.copiedPanelOpen)
        assertEquals("My reply", vm.state.value.draft)
        vm.dispatch(KeyboardAction.CloseCopiedSentence)
        assertFalse(vm.state.value.copiedPanelOpen); assertEquals(2, vm.state.value.copiedSentences.size)
        assertEquals("ms", vm.state.value.target)
        vm.endSession(); vm.beginSession(false, false, "Send")
        vm.pasteReplyReference("Another English sentence."); advanceUntilIdle()
        assertEquals(3, vm.state.value.copiedSentences.size)
        vm.dispatch(KeyboardAction.SelectCopiedSentence(0)); advanceUntilIdle()
        assertEquals("ms", vm.state.value.target)
        vm.dispatch(KeyboardAction.ClearCopiedSentences)
        assertTrue(vm.state.value.copiedPanelOpen); assertTrue(vm.state.value.copiedSentences.isEmpty())
        vm.dispatch(KeyboardAction.CloseCopiedSentence)
        vm.endSession(); vm.beginSession(false, false, "Send"); vm.openCopiedPanel()
        assertTrue(vm.state.value.copiedPanelOpen); assertTrue(vm.state.value.copiedSentences.isEmpty())
        vm.pasteReplyReference("Where are you now?"); advanceUntilIdle()
        assertEquals(1, vm.state.value.copiedSentences.size)
    }
    @Test fun newClipboardImportsOnOpenButClearedClipboardDoesNotReturn() = runTest(dispatcher) {
        var calls = 0
        val detector = object : com.lazytalk.keyboard.feature.copiedlanguage.SentenceLanguageDetector {
            override suspend fun identify(text: String): List<Pair<String, Float>> { calls++; return listOf("ms" to .99f) }
        }
        val vm = KeyboardViewModel(FakeSettings(), FakeLanguages(), TranslateTextUseCase(repository), detector)
        runCurrent(); vm.beginSession(false, false, "Send")
        vm.openCopiedPanelWithClipboard("Awak dekat mana sekarang?"); advanceUntilIdle()
        assertEquals(1, vm.state.value.copiedSentences.size); assertEquals(1, calls)
        vm.dispatch(KeyboardAction.CloseCopiedSentence)
        vm.endSession(); vm.beginSession(false, false, "Send")
        vm.openCopiedPanelWithClipboard("Another complete sentence."); advanceUntilIdle()
        assertEquals(2, vm.state.value.copiedSentences.size)
        vm.dispatch(KeyboardAction.ClearCopiedSentences)
        vm.dispatch(KeyboardAction.CloseCopiedSentence)
        vm.endSession(); vm.beginSession(false, false, "Send")
        vm.openCopiedPanelWithClipboard("Another complete sentence."); advanceUntilIdle()
        assertTrue(vm.state.value.copiedSentences.isEmpty()); assertEquals(2, calls)
        vm.openCopiedPanelWithClipboard("A different newly copied sentence."); advanceUntilIdle()
        assertEquals(listOf("A different newly copied sentence."), vm.state.value.copiedSentences)
        vm.beginSession(true, false, "Done")
        vm.openCopiedPanelWithClipboard("Secure text must be ignored."); advanceUntilIdle()
        assertTrue(vm.state.value.copiedSentences.isEmpty()); assertEquals(3, calls)
    }
    @Test fun multiplePastedSentencesRequireChoiceAndSecurePasteDoesNothing() = runTest(dispatcher) {
        var calls = 0
        val detector = object : com.lazytalk.keyboard.feature.copiedlanguage.SentenceLanguageDetector {
            override suspend fun identify(text: String): List<Pair<String, Float>> { calls++; return listOf("ms" to .99f) }
        }
        val vm = KeyboardViewModel(FakeSettings(), FakeLanguages(), TranslateTextUseCase(repository), detector)
        runCurrent(); vm.beginSession(false, false, "Send")
        vm.pasteReplyReference("Where are you now?\nAwak dekat mana sekarang?"); advanceUntilIdle()
        assertEquals(0, calls); assertTrue(vm.state.value.copiedPanelOpen)
        vm.beginSession(true, false, "Done"); vm.pasteReplyReference("Awak dekat mana sekarang?")
        advanceUntilIdle(); assertEquals(0, calls); assertFalse(vm.state.value.draftMode)
    }
    @Test fun closingPreviewCancelsRecognitionAndDiscardsCopiedText() = runTest(dispatcher) {
        val detector = object : com.lazytalk.keyboard.feature.copiedlanguage.SentenceLanguageDetector {
            override suspend fun identify(text: String): List<Pair<String, Float>> { delay(100); return listOf("ms" to .99f) }
        }
        val vm = KeyboardViewModel(FakeSettings(), FakeLanguages(), TranslateTextUseCase(repository), detector)
        runCurrent(); vm.beginSession(false, false, "Send"); vm.prepareCopiedText("Awak dekat mana sekarang?")
        vm.dispatch(KeyboardAction.ConfirmCopiedSentence); runCurrent()
        vm.dispatch(KeyboardAction.CloseCopiedSentence); advanceUntilIdle()
        assertEquals("en", vm.state.value.target); assertEquals(1, vm.state.value.copiedSentences.size)
        assertFalse(vm.state.value.detectingCopied)
    }
    @Test fun secureSessionErasesPreviewAndCannotReceivePendingResult() = runTest(dispatcher) {
        val detector = object : com.lazytalk.keyboard.feature.copiedlanguage.SentenceLanguageDetector {
            override suspend fun identify(text: String): List<Pair<String, Float>> { delay(100); return listOf("ms" to .99f) }
        }
        val vm = KeyboardViewModel(FakeSettings(), FakeLanguages(), TranslateTextUseCase(repository), detector)
        runCurrent(); vm.beginSession(false, false, "Send"); vm.prepareCopiedText("Awak dekat mana sekarang?")
        vm.dispatch(KeyboardAction.ConfirmCopiedSentence); runCurrent(); vm.beginSession(true, false, "Done")
        advanceUntilIdle(); vm.prepareCopiedText("This should not be read.")
        assertFalse(vm.state.value.copiedPanelOpen); assertTrue(vm.state.value.copiedSentences.isEmpty())
        assertEquals("en", vm.state.value.target)
    }
    @Test fun uncertainOrShortSentenceNeverChangesTheTarget() = runTest(dispatcher) {
        var calls = 0
        val detector = object : com.lazytalk.keyboard.feature.copiedlanguage.SentenceLanguageDetector {
            override suspend fun identify(text: String): List<Pair<String, Float>> { calls++; return listOf("ms" to .55f) }
        }
        val vm = KeyboardViewModel(FakeSettings(), FakeLanguages(), TranslateTextUseCase(repository), detector)
        runCurrent(); vm.beginSession(false, false, "Send"); vm.prepareCopiedText("OK.")
        vm.dispatch(KeyboardAction.ConfirmCopiedSentence); advanceUntilIdle(); assertEquals(0, calls)
        vm.prepareCopiedText("Awak dekat mana sekarang?"); vm.dispatch(KeyboardAction.ConfirmCopiedSentence); advanceUntilIdle()
        assertEquals("en", vm.state.value.target); assertNotNull(vm.state.value.copiedError)
    }
    @Test fun successfulTranslationRechecksCatalogAndClearsOldConnectionWarning() = runTest(dispatcher) {
        val languages = object : com.lazytalk.keyboard.core.data.LanguageRepository by FakeLanguages() {
            var offline = true
            var refreshes = 0
            override suspend fun refresh() { refreshes++; if (offline) throw java.io.IOException("offline") }
        }
        val vm = KeyboardViewModel(FakeSettings(), languages, TranslateTextUseCase(repository))
        runCurrent(); assertNotNull(vm.state.value.languageError)
        languages.offline = false
        vm.beginSession(false, false, "Send"); vm.importDraft("hello")
        vm.dispatch(KeyboardAction.Translate); advanceUntilIdle()
        assertNull(vm.state.value.languageError); assertFalse(vm.state.value.languagesLoading)
        assertEquals(2, languages.refreshes); assertEquals(2, vm.state.value.languages.size)
        assertFalse(vm.state.value.candidates.isEmpty())
    }
    @Test fun sameCapabilityListIsNotLostWhenSettingsChangeOrRefreshFails() = runTest(dispatcher) {
        val settings = FakeSettings()
        val languages = object : com.lazytalk.keyboard.core.data.LanguageRepository by FakeLanguages() {
            var offline = false
            override suspend fun refresh() { if (offline) throw java.io.IOException("offline") }
        }
        val vm = KeyboardViewModel(settings, languages, TranslateTextUseCase(repository))
        runCurrent()
        settings.update { it.copy(baseUrl = "https://new.example") }
        advanceUntilIdle()
        assertEquals(2, vm.state.value.languages.size)
        languages.offline = true
        vm.refreshLanguages()
        advanceUntilIdle()
        assertEquals(2, vm.state.value.languages.size)
        assertNotNull(vm.state.value.languageError)
    }
    @Test fun reopeningPickerRecoversFromInitialBackendFailure() = runTest(dispatcher) {
        val languages = object : com.lazytalk.keyboard.core.data.LanguageRepository by FakeLanguages() {
            override val languages = kotlinx.coroutines.flow.MutableStateFlow<List<Language>>(emptyList())
            var calls = 0
            override suspend fun refresh() {
                if (++calls == 1) throw java.io.IOException("offline")
                languages.value = listOf(Language("iban", "Iban", "Iban", "beta"))
            }
        }
        val vm = KeyboardViewModel(FakeSettings(), languages, TranslateTextUseCase(repository))
        runCurrent()
        assertNotNull(vm.state.value.languageError)
        assertTrue(vm.state.value.languages.isEmpty())
        vm.dispatch(KeyboardAction.OpenPicker(false))
        advanceUntilIdle()
        assertEquals("iban", vm.state.value.languages.single().code)
        assertNull(vm.state.value.languageError)
        assertFalse(vm.state.value.languagesLoading)
    }
    @Test fun convertsAndCommitsCandidateThenClearsDraft() = runTest(dispatcher) {
        val vm = model(repository); runCurrent()
        vm.beginSession(false, false, "Send"); vm.importDraft("hello")
        vm.dispatch(KeyboardAction.Translate); advanceUntilIdle()
        assertEquals(listOf("你好"), vm.state.value.candidates)
        vm.dispatch(KeyboardAction.Candidate("你好"))
        assertEquals(KeyboardEffect.Commit("你好"), vm.effects.first())
        assertEquals("", vm.state.value.draft)
    }
    @Test fun enteringSecureFieldCancelsInFlightAndErasesText() = runTest(dispatcher) {
        val vm = model(repository); runCurrent(); vm.beginSession(false, false, "Enter")
        vm.importDraft("private"); vm.dispatch(KeyboardAction.Translate); runCurrent()
        vm.beginSession(true, true, "Done"); advanceUntilIdle()
        assertTrue(vm.state.value.candidates.isEmpty()); assertEquals("", vm.state.value.draft)
        vm.importDraft("secret"); assertEquals("", vm.state.value.draft)
        vm.dispatch(KeyboardAction.Key("1")); assertEquals(KeyboardEffect.Commit("1"), vm.effects.first())
    }
    @Test fun changingDraftInvalidatesOldResult() = runTest(dispatcher) {
        val vm = model(repository); runCurrent(); vm.beginSession(false, false, "Enter")
        vm.importDraft("hello"); vm.dispatch(KeyboardAction.Translate); runCurrent()
        vm.importDraft("changed"); advanceUntilIdle(); assertTrue(vm.state.value.candidates.isEmpty())
    }
    @Test fun draftKeysAndPasteReplaceSelectionAtCursor() = runTest(dispatcher) {
        val vm = model(repository); runCurrent(); vm.beginSession(false, false, "Send"); vm.importDraft("hello world")
        vm.dispatch(KeyboardAction.EditDraft("hello world", androidx.compose.ui.text.TextRange(5)))
        vm.dispatch(KeyboardAction.Key(",")); assertEquals("hello, world", vm.state.value.draft)
        assertEquals(6, vm.state.value.draftSelection.start)
        vm.dispatch(KeyboardAction.EditDraft(vm.state.value.draft, androidx.compose.ui.text.TextRange(12, 7)))
        vm.insertDraftText("世界"); assertEquals("hello, 世界", vm.state.value.draft)
        vm.dispatch(KeyboardAction.Backspace); assertEquals("hello, 世", vm.state.value.draft)
        vm.dispatch(KeyboardAction.EditDraft(vm.state.value.draft, androidx.compose.ui.text.TextRange(0, 5)))
        vm.dispatch(KeyboardAction.Enter); assertEquals("\n, 世", vm.state.value.draft)
    }
    @Test fun draftBackspaceAtCursorPreservesFollowingTextAndDeletesWholeEmoji() = runTest(dispatcher) {
        val vm = model(repository); runCurrent(); vm.beginSession(false, false, "Send"); vm.importDraft("a😀b")
        vm.dispatch(KeyboardAction.EditDraft("a😀b", androidx.compose.ui.text.TextRange(3)))
        vm.dispatch(KeyboardAction.Backspace); assertEquals("ab", vm.state.value.draft)
        assertEquals(1, vm.state.value.draftSelection.start)
        vm.dispatch(KeyboardAction.EditDraft("ab", androidx.compose.ui.text.TextRange(0)))
        vm.dispatch(KeyboardAction.Backspace); assertEquals("ab", vm.state.value.draft)
        vm.importDraft("x".repeat(4000)); vm.insertDraftText("extra")
        assertEquals(4000, vm.state.value.draft.length)
        vm.beginSession(true, false, "Done"); vm.dispatch(KeyboardAction.EditDraft("secret", androidx.compose.ui.text.TextRange(6)))
        assertEquals("", vm.state.value.draft)
    }
    @Test fun shiftCapsAndUnicodeBackspace() = runTest(dispatcher) {
        val vm = model(repository); runCurrent(); vm.beginSession(false, false, "Enter")
        vm.dispatch(KeyboardAction.ToggleDraft); vm.dispatch(KeyboardAction.Shift); vm.dispatch(KeyboardAction.Key("a"))
        assertEquals("A", vm.state.value.draft); assertEquals(ShiftState.OFF, vm.state.value.shift)
        vm.importDraft("a😀"); vm.dispatch(KeyboardAction.Backspace); assertEquals("a", vm.state.value.draft)
    }
    @Test fun queuedKeyFromPreviousSessionCannotReachNewEditor() = runTest(dispatcher) {
        val vm = model(repository); runCurrent(); vm.beginSession(false, false, "Enter")
        vm.dispatch(KeyboardAction.Key("old"))
        vm.beginSession(true, false, "Done")
        vm.dispatch(KeyboardAction.Key("new"))
        assertEquals(KeyboardEffect.Commit("new"), vm.effects.first())
    }
}
