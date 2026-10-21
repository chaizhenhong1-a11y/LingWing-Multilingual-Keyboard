package com.lazytalk.keyboard.feature.keyboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazytalk.keyboard.core.common.*
import com.lazytalk.keyboard.core.data.*
import com.lazytalk.keyboard.core.model.*
import com.lazytalk.keyboard.feature.keyboard.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import com.lazytalk.keyboard.feature.copiedlanguage.*

class KeyboardViewModel(private val settingsRepo: SettingsRepository, private val languageRepo: LanguageRepository,
    private val translate: TranslateTextUseCase, private val sentenceDetector: SentenceLanguageDetector? = null, private val typing: TypingSuggestions = TypingSuggestions.None) : ViewModel() {
    private val pendingTyping = java.util.ArrayDeque<KeyboardAction>()
    private var typingInput = ""
    private var typingRevision = 0L
    private var typingJob: Job? = null
    fun cancelComposition() {
        typingRevision++; typingJob?.cancel();typingInput="";pendingTyping.clear()
        mutable.update { it.copy(composition="", typingCandidates=emptyList(), typingLoading=false, typingError=null) }
    }
    private fun typingDisplay() = when(state.value.typingLayout.engine) {
        "kana" -> KanaInput.render(typingInput)
        "hangul" -> HangulInput.render(typingInput)
        "telex" -> TelexInput.render(typingInput)
        else -> typingInput
    }
    private fun updateComposition() {
        val revision=++typingRevision;typingJob?.cancel()
        val display=typingDisplay();val engine=state.value.typingLayout.engine;val input=typingInput
        if(!state.value.draftMode) effect(KeyboardEffect.Composing(display))
        mutable.update { it.copy(composition=display, typingCandidates=emptyList(), typingLoading=input.isNotEmpty(), typingError=null) }
        if(input.isEmpty())return
        typingJob=viewModelScope.launch {
            try {
                val result=withContext(Dispatchers.Default) { typing.candidates(engine,input) }
                if(revision==typingRevision) {
                    mutable.update { it.copy(typingCandidates=result.ifEmpty { listOf(display) }, typingLoading=false) }
                    val queued=pendingTyping.toList();pendingTyping.clear();queued.forEach(::dispatch)
                }
            } catch(e: CancellationException) { throw e }
            catch(_: Exception) { if(revision==typingRevision) {
                mutable.update { it.copy(typingLoading=false, typingCandidates=listOf(display), typingError="词库暂不可用，可选择原文") }
                val queued=pendingTyping.toList();pendingTyping.clear();queued.forEach(::dispatch)
            } }
        }
    }
    private fun acceptComposition(text: String? = null): Boolean {
        val s=state.value
        if(typingInput.isEmpty())return true
        if(text==null && s.typingLoading)return false
        val value=text ?: s.typingCandidates.firstOrNull() ?: typingDisplay()
        if(s.draftMode && !s.secure) insertDraftText(value) else effect(KeyboardEffect.Commit(value))
        cancelComposition();return true
    }
    private val mutable = MutableStateFlow(KeyboardUiState())
    val state = mutable.asStateFlow()
    private data class SessionEffect(val session: Long, val effect: KeyboardEffect, val revision: Long? = null, val typingRevision: Long? = null)
    private var session = 0L
    private val events = Channel<SessionEffect>(Channel.UNLIMITED)
    val effects = events.receiveAsFlow().filter { it.session == session && (it.revision == null || it.revision == generation) && (it.typingRevision == null || it.typingRevision == typingRevision) }.map { it.effect }
    private var request: Job? = null
    private var languageRequest: Job? = null
    private var generation = 0L
    private var configuration: Triple<TranslationMode, String, String>? = null
    private var previousSettings: UserSettings? = null
    private var copiedRequest: Job? = null
    private var copiedGeneration = 0L
    private var liveTimer: Job? = null
    private var liveOutputActive = false
    private data class LiveInput(val session: Long, val text: String, val source: String, val target: String,
        val tone: Tone, val mode: TranslationMode, val url: String, val enabled: Boolean, val active: Boolean)
    private var lastLiveInput: LiveInput? = null
    private fun scheduleLiveTranslation() {
        val s = state.value
        val enabled = s.liveTranslation && s.draftMode && !s.secure && !s.liveBlocked && s.settings.mode == TranslationMode.REMOTE
        val active = enabled && !s.copiedPanelOpen && !s.layoutPicker && s.picker == null
        val next = LiveInput(session, s.draft, s.source, s.target, s.tone, s.settings.mode, s.settings.baseUrl, enabled, active)
        if (next == lastLiveInput) return
        lastLiveInput = next
        liveTimer?.cancel()
        if (!active) {
            if (liveOutputActive) {
                invalidate()
                if (!enabled) { effect(KeyboardEffect.EndLiveTranslation); liveOutputActive = false }
            }
            return
        }
        if (!liveOutputActive) { effect(KeyboardEffect.BeginLiveTranslation); liveOutputActive = true }
        liveTimer = viewModelScope.launch {
            delay(500)
            if (state.value.draft.isBlank()) effect(KeyboardEffect.UpdateLiveTranslation(""))
            else convert(live = true)
        }
    }
    fun liveInsertionFailed() {
        invalidate()
        mutable.update { it.copy(liveBlocked = true, error = "输入框已改变，已暂停自动写入。关闭再开启实时翻译可继续。") }
        scheduleLiveTranslation()
    }
    private var lastClipboardFingerprint: String? = null
    private fun clipboardFingerprint(text: String): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    private fun clearCopied(erase: Boolean = true) {
        copiedGeneration++; copiedRequest?.cancel()
        mutable.update { it.copy(copiedPanelOpen = false, copiedSentences = if (erase) emptyList() else it.copiedSentences, selectedCopiedSentence = 0,
            detectingCopied = false, copiedError = null) }
    }
    fun prepareCopiedText(text: String?) {
        if (state.value.secure) return
        clearCopied()
        val choices = CopiedSentencePolicy.sentences(text.orEmpty())
        mutable.update { it.copy(copiedPanelOpen = true, copiedSentences = choices, replyLanguageNotice = null,
            copiedError = when {
                text != null && text.length > 4000 -> "文字过长，请只复制要识别的一句话。"
                choices.isEmpty() -> "剪贴板没有可识别的普通文字，或内容被标记为敏感。"
                else -> null
            }) }
        scheduleLiveTranslation()
    }
    fun openCopiedPanel() {
        if (state.value.secure) return
        mutable.update { it.copy(copiedPanelOpen = true, copiedError = null) }
        scheduleLiveTranslation()
    }
    fun openCopiedPanelWithClipboard(text: String?) {
        if (state.value.secure) return
        openCopiedPanel()
        if (!text.isNullOrBlank() && clipboardFingerprint(text) != lastClipboardFingerprint)
            pasteReplyReference(text)
    }
    fun pasteReplyReference(text: String?) {
        if (state.value.secure) return
        if (!text.isNullOrBlank()) lastClipboardFingerprint = clipboardFingerprint(text)
        val previous = state.value.copiedSentences
        prepareCopiedText(text)
        val incoming = state.value.copiedSentences
        val combined = (previous + incoming).distinct().takeLast(50)
        mutable.update { it.copy(draftMode = true, liveTranslation = it.liveTranslation || it.settings.mode == TranslationMode.REMOTE, copiedSentences = combined,
            selectedCopiedSentence = combined.indexOf(incoming.firstOrNull()).coerceAtLeast(0)) }
        if (incoming.size == 1 && state.value.copiedError == null)
            identifyCopiedSentence()
        scheduleLiveTranslation()
    }
    private fun identifyCopiedSentence() {
        val s = state.value
        val text = s.copiedSentences.getOrNull(s.selectedCopiedSentence) ?: return
        if (s.secure || !s.copiedPanelOpen || s.detectingCopied) return
        if (text.count(Char::isLetter) < 6) {
            mutable.update { it.copy(copiedError = "句子太短，无法可靠识别，请复制更完整的一句。") }; return
        }
        val token = ++copiedGeneration
        mutable.update { it.copy(detectingCopied = true, copiedError = null) }
        copiedRequest = viewModelScope.launch {
            try {
                val scores = sentenceDetector?.identify(text) ?: emptyList()
                if (token != copiedGeneration || state.value.secure) return@launch
                val code = CopiedSentencePolicy.language(scores, state.value.languages.map { it.code }.toSet())
                if (code == null) mutable.update { it.copy(detectingCopied = false, copiedError = "语言不确定或本地模型不支持，请手动选择目标语言。") }
                else {
                    val name = state.value.languages.find { it.code == code }?.nativeName ?: code
                    invalidate()
                    mutable.update { it.copy(target = code, source = "auto", draftMode = true, detectingCopied = false,
                        replyLanguageNotice = "已识别 $name，请输入你的回复") }
                    scheduleLiveTranslation()
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (token == copiedGeneration) mutable.update { it.copy(detectingCopied = false, copiedError = "本地识别暂时不可用，请手动选择语言。") }
            }
        }
    }
    init {
        viewModelScope.launch {
            settingsRepo.settings.collect { settings ->
                val config = Triple(settings.mode, settings.baseUrl, settings.backendToken)
                val first = configuration == null
                val defaultsChanged = first || previousSettings?.target != settings.target || previousSettings?.tone != settings.tone
                if (defaultsChanged) invalidate()
                mutable.update { it.copy(settings = settings, typingLayout = TypingLayouts.find(settings.typingLayoutId),
                    target = if (first || previousSettings?.target != settings.target) settings.target else it.target,
                    tone = if (first || previousSettings?.tone != settings.tone) settings.tone else it.tone) }
                previousSettings = settings
                if (config != configuration) {
                    invalidate(); configuration = config
                    refreshLanguages()
                }
                scheduleLiveTranslation()
            }
        }
        viewModelScope.launch { languageRepo.languages.collect { list -> mutable.update { it.copy(languages = list) } } }
    }
    fun refreshLanguages() {
        languageRequest?.cancel()
        mutable.update { it.copy(languagesLoading = true, languageError = null) }
        languageRequest = viewModelScope.launch {
        try {
            languageRepo.refresh()
            val list = languageRepo.languages.first()
            mutable.update { it.copy(languages = list, languageError = null, languagesLoading = false) }
        }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            val list = languageRepo.languages.first()
            mutable.update { it.copy(languageError = "语言列表暂未更新，可继续使用现有语言。", languages = list, languagesLoading = false) }
        }
    } }
    private fun invalidate() { generation++; request?.cancel(); mutable.update { it.copy(loading = false, candidates = emptyList(), detected = null, error = null) } }
    fun beginSession(secure: Boolean, numeric: Boolean, enterLabel: String) {
        resetSession(secure, numeric, enterLabel, secure)
    }
    private fun resetSession(secure: Boolean, numeric: Boolean, enterLabel: String, eraseReferences: Boolean) {
        cancelComposition()
        liveTimer?.cancel(); liveOutputActive = false; lastLiveInput = null
        clearCopied(eraseReferences)
        session++
        invalidate()
        mutable.update { it.copy(draft = "", draftSelection = androidx.compose.ui.text.TextRange.Zero, draftMode = false, liveBlocked = false, secure = secure, numeric = numeric,
            page = if (numeric) KeyPage.NUMBERS else KeyPage.LETTERS, shift = ShiftState.OFF, picker = null, layoutPicker = false, enterLabel = enterLabel, replyLanguageNotice = null) }
    }
    fun endSession() { resetSession(true, false, "Enter", false) }
    private fun effect(value: KeyboardEffect) { events.trySend(SessionEffect(session, value,
        if (value is KeyboardEffect.UpdateLiveTranslation) generation else null, if(value is KeyboardEffect.Composing) typingRevision else null)) }
    fun dispatch(action: KeyboardAction) {
        val s = state.value
        if(action!=KeyboardAction.Clear && action !is KeyboardAction.TypingCandidate) {
            val needsCommit = when(action) {
                is KeyboardAction.Key -> if(s.typingLayout.engine=="hangul") action.text.any { it !in 'ㄱ'..'ㅣ' }
                    else action.text.any { !it.isLetter() && it!='\'' && !(it=='-' && s.typingLayout.engine=="kana") }
                KeyboardAction.Enter -> s.typingLayout.engine != "pinyin"
                is KeyboardAction.Page, KeyboardAction.ToggleDraft, KeyboardAction.OpenLayouts, KeyboardAction.Translate -> true
                else -> false
            }
            if(pendingTyping.isNotEmpty() || (s.typingLoading && typingInput.isNotEmpty() && needsCommit)) {
                pendingTyping.addLast(action);return
            }
        }
        when (action) {
            is KeyboardAction.TypingCandidate -> if(action.text in s.typingCandidates) acceptComposition(action.text)
            KeyboardAction.OpenLayouts -> { if(acceptComposition()) mutable.update { it.copy(layoutPicker = true) } }
            KeyboardAction.CloseLayouts -> mutable.update { it.copy(layoutPicker = false) }
            is KeyboardAction.PickLayout -> if (TypingLayouts.all.any { it.id == action.id }) {
                mutable.update { it.copy(typingLayout = TypingLayouts.find(action.id), layoutPicker = false, page = KeyPage.LETTERS, shift = ShiftState.OFF) }
                persist { settingsRepo.update { it.copy(typingLayoutId = action.id) } }
            }
            is KeyboardAction.SelectCopiedSentence -> if (action.index in s.copiedSentences.indices) {
                copiedGeneration++; copiedRequest?.cancel()
                mutable.update { it.copy(selectedCopiedSentence = action.index, detectingCopied = false, copiedError = null) }
                identifyCopiedSentence()
            }
            KeyboardAction.ConfirmCopiedSentence -> identifyCopiedSentence()
            KeyboardAction.CloseCopiedSentence -> clearCopied(false)
            KeyboardAction.ClearCopiedSentences -> { clearCopied(); mutable.update { it.copy(copiedPanelOpen = true) } }
            is KeyboardAction.Key -> {
                val text = if (s.page == KeyPage.LETTERS && s.shift != ShiftState.OFF) action.text.uppercase(java.util.Locale.forLanguageTag(s.typingLayout.tag)) else action.text
                if(s.typingLayout.engine!="direct" && !s.secure && s.page==KeyPage.LETTERS) {
                    val phonetic=if(s.typingLayout.engine=="hangul") text.all { it in 'ㄱ'..'ㅣ' } else text.all { it in 'a'..'z' || it in 'A'..'Z' || it=='\'' || (it=='-' && s.typingLayout.engine=="kana") }
                    if(phonetic && typingInput.length<80) {
                        typingInput+=if(s.typingLayout.engine in listOf("hangul","telex")) text else text.lowercase()
                        updateComposition()
                        if(s.shift==ShiftState.ON) mutable.update { it.copy(shift=ShiftState.OFF) }
                        return
                    }
                    if(typingInput.isNotEmpty()) {
                        if(!acceptComposition())return
                        if(text==" " && s.typingLayout.engine in listOf("pinyin","kana"))return
                    }
                }
                if (s.draftMode && !s.secure) insertDraftText(text) else effect(KeyboardEffect.Commit(text))
                if (s.shift == ShiftState.ON) mutable.update { it.copy(shift = ShiftState.OFF) }
            }
            KeyboardAction.Backspace -> if(typingInput.isNotEmpty()) { typingInput=typingInput.dropLast(1);updateComposition() } else if (s.draftMode && !s.secure) {
                val start = s.draftSelection.min.coerceIn(0, s.draft.length)
                val end = s.draftSelection.max.coerceIn(start, s.draft.length)
                val from = if (start == end && start > 0) s.draft.offsetByCodePoints(start, -1) else start
                setDraft(s.draft.removeRange(from, end), androidx.compose.ui.text.TextRange(from))
            } else effect(KeyboardEffect.Delete)
            KeyboardAction.Enter -> if(typingInput.isNotEmpty()) {
                acceptComposition(if(s.typingLayout.engine=="pinyin") typingInput else null)
            } else if (s.draftMode && !s.secure) insertDraftText("\n") else effect(KeyboardEffect.Enter)
            KeyboardAction.Shift -> mutable.update { it.copy(shift = ShiftState.entries[(it.shift.ordinal + 1) % 3]) }
            is KeyboardAction.Page -> { if(acceptComposition()) mutable.update { it.copy(page = action.page) } }
            KeyboardAction.ToggleDraft -> if (!s.secure && acceptComposition()) { invalidate(); mutable.update { it.copy(draftMode = !it.draftMode,
                liveTranslation = if (!it.draftMode) true else it.liveTranslation, liveBlocked = false) } }
            is KeyboardAction.Draft -> if (!s.secure) { invalidate(); setDraft(action.text) }
            is KeyboardAction.EditDraft -> if (!s.secure) setDraft(action.text, action.selection)
            KeyboardAction.Clear -> { cancelComposition(); setDraft("") }
            KeyboardAction.Translate -> { if(acceptComposition()) { liveTimer?.cancel(); convert(live = s.liveTranslation) } }
            KeyboardAction.ToggleLiveTranslation -> { invalidate(); mutable.update { it.copy(liveTranslation = !it.liveTranslation, liveBlocked = false) } }
            KeyboardAction.Swap -> {
                val pair = SwapLanguagesUseCase()(s.source, s.target, s.detected)
                if (pair == null) mutable.update { it.copy(error = "Convert once to detect the source language before swapping.") }
                else {
                    invalidate(); mutable.update { it.copy(source = pair.source, target = pair.target) }
                }
            }
            is KeyboardAction.ToneChanged -> { invalidate(); mutable.update { it.copy(tone = action.tone) } }
            is KeyboardAction.OpenPicker -> if (!s.secure) {
                mutable.update { it.copy(picker = action.source) }
                if (s.languages.isEmpty() && !s.languagesLoading) refreshLanguages()
            }
            KeyboardAction.ClosePicker -> mutable.update { it.copy(picker = null) }
            is KeyboardAction.PickLanguage -> {
                clearCopied(false)
                invalidate()
                mutable.update { if (it.picker == true) it.copy(source = action.code, picker = null)
                    else it.copy(target = action.code, picker = null, replyLanguageNotice = null) }
                if (action.code != "auto") persist { languageRepo.used(action.code) }
            }
            is KeyboardAction.Favorite -> persist { languageRepo.favorite(action.code) }
            is KeyboardAction.Candidate -> if (!s.secure && action.text in s.candidates && !s.loading) {
                effect(KeyboardEffect.Commit(action.text)); invalidate(); mutable.update { it.copy(draft = "", draftSelection = androidx.compose.ui.text.TextRange.Zero, draftMode = false) }
            }
        }
        scheduleLiveTranslation()
    }
    private fun persist(block: suspend () -> Unit) { viewModelScope.launch {
        try { block() } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(error = "Settings could not be saved.") } }
    } }
    private fun setDraft(text: String, selection: androidx.compose.ui.text.TextRange = androidx.compose.ui.text.TextRange(text.length)) {
        val bounded = text.take(4000)
        if (bounded != state.value.draft) invalidate()
        mutable.update { it.copy(draft = bounded, draftSelection = androidx.compose.ui.text.TextRange(
            selection.start.coerceIn(0, bounded.length), selection.end.coerceIn(0, bounded.length))) }
        scheduleLiveTranslation()
    }
    fun insertDraftText(text: String) {
        val s = state.value
        if (s.secure) return
        val start = s.draftSelection.min.coerceIn(0, s.draft.length)
        val end = s.draftSelection.max.coerceIn(start, s.draft.length)
        val inserted = text.take((4000 - s.draft.length + end - start).coerceAtLeast(0))
        setDraft(s.draft.replaceRange(start, end, inserted), androidx.compose.ui.text.TextRange(start + inserted.length))
    }
    fun importDraft(text: String) { if (!state.value.secure) { setDraft(text); mutable.update { it.copy(draftMode = true,
        liveTranslation = it.liveTranslation || it.settings.mode == TranslationMode.REMOTE) }; scheduleLiveTranslation() } }
    private fun convert(live: Boolean = false) {
        val s = state.value
        if (s.secure || (live && (s.liveBlocked || !s.draftMode || s.copiedPanelOpen || s.picker != null || s.settings.mode != TranslationMode.REMOTE))) return
        invalidate()
        val token = generation
        mutable.update { it.copy(loading = true) }
        request = viewModelScope.launch {
            try {
                val result = translate(TranslationRequest(s.draft, s.source, s.target, s.tone), s.secure)
                if (generation == token && !state.value.secure) {
                    val refreshCatalog = state.value.languageError != null
                    mutable.update { it.copy(detected = result.detectedLanguage, candidates = if (live) emptyList() else result.candidates,
                        loading = false, languageError = null) }
                    if (refreshCatalog) refreshLanguages()
                    if (live) result.candidates.firstOrNull()?.let { effect(KeyboardEffect.UpdateLiveTranslation(it)) }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: AppException) { if (generation == token) mutable.update { it.copy(loading = false, error = e.error.message) } }
        }
    }
    override fun onCleared() { copiedRequest?.cancel(); sentenceDetector?.close(); super.onCleared() }
}
