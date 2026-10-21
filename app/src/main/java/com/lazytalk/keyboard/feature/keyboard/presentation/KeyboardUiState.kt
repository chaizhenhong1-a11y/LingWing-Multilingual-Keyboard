package com.lazytalk.keyboard.feature.keyboard.presentation
import com.lazytalk.keyboard.core.model.*
import androidx.compose.ui.text.TextRange
enum class ShiftState { OFF, ON, CAPS }
enum class KeyPage { LETTERS, NUMBERS, SYMBOLS, EMOJI }
data class KeyboardUiState(
    val typingLayout: TypingLayout = TypingLayouts.find("en"), val layoutPicker: Boolean = false,
    val composition: String = "", val typingCandidates: List<String> = emptyList(),
    val typingLoading: Boolean = false, val typingError: String? = null,
    val source: String = "auto", val target: String = "en", val detected: String? = null,
    val tone: Tone = Tone.NATURAL, val draft: String = "", val draftMode: Boolean = false,
    val draftSelection: TextRange = TextRange.Zero,
    val liveTranslation: Boolean = false, val liveBlocked: Boolean = false,
    val candidates: List<String> = emptyList(), val loading: Boolean = false, val error: String? = null,
    val secure: Boolean = false, val shift: ShiftState = ShiftState.OFF, val page: KeyPage = KeyPage.LETTERS,
    val languages: List<Language> = emptyList(), val settings: UserSettings = UserSettings(),
    val picker: Boolean? = null, val languageError: String? = null, val languagesLoading: Boolean = false,
    val numeric: Boolean = false, val enterLabel: String = "Enter",
    val copiedPanelOpen: Boolean = false, val copiedSentences: List<String> = emptyList(),
    val selectedCopiedSentence: Int = 0, val detectingCopied: Boolean = false,
    val copiedError: String? = null, val replyLanguageNotice: String? = null
)
sealed interface KeyboardAction {
    data class SelectCopiedSentence(val index: Int) : KeyboardAction
    data object ConfirmCopiedSentence : KeyboardAction
    data object CloseCopiedSentence : KeyboardAction
    data object ClearCopiedSentences : KeyboardAction
    data class Key(val text: String) : KeyboardAction
    data class TypingCandidate(val text: String) : KeyboardAction
    data object OpenLayouts : KeyboardAction
    data object CloseLayouts : KeyboardAction
    data class PickLayout(val id: String) : KeyboardAction
    data object Backspace : KeyboardAction
    data object Enter : KeyboardAction
    data object Shift : KeyboardAction
    data class Page(val page: KeyPage) : KeyboardAction
    data object ToggleDraft : KeyboardAction
    data class Draft(val text: String) : KeyboardAction
    data class EditDraft(val text: String, val selection: TextRange) : KeyboardAction
    data object Translate : KeyboardAction
    data object ToggleLiveTranslation : KeyboardAction
    data object Swap : KeyboardAction
    data class ToneChanged(val tone: Tone) : KeyboardAction
    data class OpenPicker(val source: Boolean) : KeyboardAction
    data class PickLanguage(val code: String) : KeyboardAction
    data class Favorite(val code: String) : KeyboardAction
    data object ClosePicker : KeyboardAction
    data class Candidate(val text: String) : KeyboardAction
    data object Clear : KeyboardAction
}
sealed interface KeyboardEffect {
    data object BeginLiveTranslation : KeyboardEffect
    data class UpdateLiveTranslation(val text: String) : KeyboardEffect
    data object EndLiveTranslation : KeyboardEffect
    data class Composing(val text: String) : KeyboardEffect
    data class Commit(val text: String) : KeyboardEffect
    data object Delete : KeyboardEffect
    data object Enter : KeyboardEffect
}
