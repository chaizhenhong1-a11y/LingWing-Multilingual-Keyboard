package com.lazytalk.keyboard.feature.keyboard.components

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lazytalk.keyboard.core.model.Language
import com.lazytalk.keyboard.feature.keyboard.presentation.*
import com.lazytalk.keyboard.ui.LazyTalkTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w411dp-h891dp-port")
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class KeyboardLayoutTest {
    @get:Rule val compose = createComposeRule()

    private fun candidatesReuseToolbar(drafting: Boolean = false) {
        var state by mutableStateOf(KeyboardUiState(draftMode=drafting))
        var selected: String?=null
        compose.setContent { LazyTalkTheme { KeyboardContent(state, { a ->
            if(a is KeyboardAction.TypingCandidate) {
                selected=a.text;state=state.copy(composition="",typingCandidates=emptyList())
            }
        }, {}, {}, {}, {}, {}) } }
        val keyBefore=compose.onNodeWithText("q").fetchSemanticsNode().boundsInRoot
        val before=keyboardHeight()
        compose.runOnIdle { state=state.copy(composition="nihao",typingCandidates=listOf("你好","你号")) }
        val keyDuring=compose.onNodeWithText("q").fetchSemanticsNode().boundsInRoot
        assertTrue(kotlin.math.abs(keyBefore.top-keyDuring.top)<1f)
        assertTrue(kotlin.math.abs(keyBefore.height-keyDuring.height)<1f)
        assertTrue(kotlin.math.abs(before-keyboardHeight())<1f)
        compose.onNodeWithContentDescription("Keyboard settings").assertDoesNotExist()
        val candidate=compose.onNodeWithText("你好").fetchSemanticsNode().boundsInRoot
        assertTrue(candidate.bottom<=keyDuring.top)
        val spelling=compose.onNodeWithText("nihao").fetchSemanticsNode().boundsInRoot
        assertTrue(spelling.bottom<=candidate.top)
        compose.onNodeWithText("你好").performClick()
        compose.runOnIdle { assertTrue(selected=="你好") }
        if(!drafting) compose.onNodeWithContentDescription("Keyboard settings").assertIsDisplayed()
        else compose.onNodeWithContentDescription("Exit translation").assertIsDisplayed()
        assertTrue(kotlin.math.abs(before-keyboardHeight())<1f)
    }
    @Test fun portraitCandidatesReplaceToolbarWithoutShrinkingKeys() = candidatesReuseToolbar()
    @Test @Config(qualifiers = "w891dp-h411dp-land")
    fun landscapeCandidatesReplaceToolbarWithoutShrinkingKeys() = candidatesReuseToolbar()
    @Test fun draftCandidatesReplaceLanguageToolbarWithoutShrinkingKeys() = candidatesReuseToolbar(true)

    @Test @Config(qualifiers = "w640dp-h320dp-land")
    fun composingDraftKeepsCandidatesAndKeysWithinSmallLandscapeWindow() {
        show(KeyboardUiState(draftMode=true, composition="nihao", typingCandidates=listOf("你好"), error="Previous translation failed"))
        compose.onNodeWithText("你好").assertIsDisplayed()
        compose.onNodeWithText("q").assertIsDisplayed()
        compose.onNodeWithText("Previous translation failed").assertDoesNotExist()
        assertTrue(keyboardHeight()<=236f)
    }

    @Test fun longPressFirstRowInsertsOnlyNumberAndEmojiReturnsToLetters() {
        var state by mutableStateOf(KeyboardUiState())
        val inserted = mutableListOf<String>()
        compose.setContent { LazyTalkTheme { KeyboardContent(state, { a ->
            when (a) {
                is KeyboardAction.Key -> inserted += a.text
                is KeyboardAction.Page -> state = state.copy(page = a.page)
                else -> Unit
            }
        }, {}, {}, {}, {}, {}) } }
        compose.onNodeWithText("q").performTouchInput { longClick() }
        compose.runOnIdle { assertTrue(inserted == listOf("1")) }
        compose.onNodeWithText("q").performTouchInput { click() }
        compose.runOnIdle { assertTrue(inserted == listOf("1", "q")) }
        compose.onNodeWithText("?123").assertIsDisplayed()
        compose.onNodeWithContentDescription("Emoji").performClick()
        compose.onNodeWithText("❤️").performClick()
        compose.runOnIdle { assertTrue(inserted.last() == "❤️") }
        compose.onNodeWithText("English").performClick()
        compose.onNodeWithText("q").assertIsDisplayed()
    }

    @Test fun layoutPickerAndThaiKeysUseIndependentTypingLanguage() {
        var state by mutableStateOf(KeyboardUiState(target = "ms"))
        compose.setContent { LazyTalkTheme { KeyboardContent(state, { a ->
            state = when (a) {
                KeyboardAction.OpenLayouts -> state.copy(layoutPicker = true)
                is KeyboardAction.PickLayout -> state.copy(layoutPicker = false,
                    typingLayout = com.lazytalk.keyboard.core.model.TypingLayouts.find(a.id))
                else -> state
            }
        }, {}, {}, {}, {}, {}) } }
        compose.onNodeWithContentDescription("当前键盘语言：English").performClick()
        "thai".forEach { compose.onNodeWithText(it.toString()).performClick() }
        compose.onNodeWithText("Thai · ไทย").performClick()
        compose.onNodeWithContentDescription("当前键盘语言：ไทย").assertIsDisplayed()
        compose.onNodeWithText("q").assertDoesNotExist()
        compose.onNodeWithText("ก").assertIsDisplayed()
        compose.runOnIdle { assertTrue(state.target == "ms") }
        assertTrue(keyboardHeight() <= 250f)
    }

    @Test @Config(qualifiers = "w891dp-h411dp-land")
    fun thaiLandscapeFitsAllRows() {
        show(KeyboardUiState(typingLayout = com.lazytalk.keyboard.core.model.TypingLayouts.find("th")))
        compose.onNodeWithText("ก").assertIsDisplayed()
        compose.onNodeWithText("ฝ").assertIsDisplayed()
        compose.onNodeWithText("Space").assertIsDisplayed()
        assertTrue(keyboardHeight() <= 280f)
    }

    @Test fun openingRecognitionDoesNotReadClipboardButExplicitPasteDoes() {
        var reads = 0
        var opens = 0
        var state by mutableStateOf(KeyboardUiState())
        compose.setContent { LazyTalkTheme { KeyboardContent(state, {}, {}, {}, {}, {}, {},
            onRecognizeCopied = { reads++ }, onOpenRecognition = { opens++ }) } }
        compose.onNodeWithContentDescription("打开剪贴板页面").performClick()
        compose.runOnIdle { assertTrue(opens == 1 && reads == 0); state = state.copy(draftMode = true) }
        compose.onNodeWithContentDescription("打开识别列表").performClick()
        compose.runOnIdle { assertTrue(opens == 2 && reads == 0); state = state.copy(copiedPanelOpen = true) }
        compose.onNodeWithText("Paste").performClick()
        compose.runOnIdle { assertTrue(reads == 1) }
    }
    @Test fun draftEditorSupportsSelectionAndTextEditingWithoutMockPrefix() {
        var state by mutableStateOf(KeyboardUiState(draftMode = true, draft = "hello world"))
        compose.setContent { LazyTalkTheme { KeyboardContent(state, { action ->
            if (action is KeyboardAction.EditDraft) state = state.copy(draft = action.text, draftSelection = action.selection)
        }, {}, {}, {}, {}, {}) } }
        compose.onNodeWithTag("draftEditor").assertIsFocused().performTextInputSelection(androidx.compose.ui.text.TextRange(0, 5))
        compose.onNodeWithTag("draftEditor").performTextInput("Hi")
        compose.runOnIdle { assertTrue(state.draft == "Hi world") }
        compose.onNodeWithText("Mock ·", substring = true).assertDoesNotExist()
    }
    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) @Config(shadows = [NoWindowMagnifierShadow::class])
    fun longPressDraftSelectsAWordForNativeTextToolbar() {
        var state by mutableStateOf(KeyboardUiState(draftMode = true, draft = "hello world"))
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("reply", "replacement"))
        compose.setContent {
            LazyTalkTheme { KeyboardContent(state, { action ->
                if (action is KeyboardAction.EditDraft) state = state.copy(draft = action.text, draftSelection = action.selection)
            }, {}, {}, {}, {}, {}) }
        }
        val point = with(compose.density) { androidx.compose.ui.geometry.Offset(24.dp.toPx(), 16.dp.toPx()) }
        compose.onNodeWithTag("draftEditor").performTouchInput { longClick(point) }
        compose.runOnIdle {
            assertTrue("selection=${state.draftSelection}", !state.draftSelection.collapsed)
        }
        // Android's floating ActionMode is outside Compose's semantics tree in Robolectric.
        // Verify real gesture selection here; the native popup itself needs phone acceptance.
        compose.runOnIdle {
            assertTrue(state.draft.substring(state.draftSelection.min, state.draftSelection.max) == "hello")
        }
    }
    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun cursorIsDrawnEvenWhenImeWindowDoesNotHaveAndroidFocus() {
        compose.mainClock.autoAdvance = false
        lateinit var host: android.view.View
        compose.setContent {
            host = androidx.compose.ui.platform.LocalView.current
            val real = androidx.compose.ui.platform.LocalWindowInfo.current
            val unfocused = remember(real) { object : androidx.compose.ui.platform.WindowInfo by real {
                override val isWindowFocused: Boolean get() = false
            } }
            CompositionLocalProvider(androidx.compose.ui.platform.LocalWindowInfo provides unfocused) {
                androidx.compose.material3.MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme(
                    primary = androidx.compose.ui.graphics.Color.Magenta)) {
                    KeyboardContent(KeyboardUiState(draftMode = true, draft = "hello",
                        draftSelection = androidx.compose.ui.text.TextRange(5)), {}, {}, {}, {}, {}, {})
                }
            }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("draftEditor").assertIsFocused()
        val bounds = compose.onNodeWithTag("draftEditor").fetchSemanticsNode().boundsInRoot
        var cursorPixels = 0
        compose.runOnIdle {
            val bitmap = android.graphics.Bitmap.createBitmap(bounds.width.toInt(), bounds.height.toInt(), android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.translate(-bounds.left, -bounds.top)
            host.draw(canvas)
            for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                val p = bitmap.getPixel(x, y)
                if (android.graphics.Color.red(p) > 230 && android.graphics.Color.green(p) < 50 && android.graphics.Color.blue(p) > 230) cursorPixels++
            }
            java.io.File(System.getProperty("java.io.tmpdir"), "lazytalk-ime-cursor.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
        assertTrue("Expected visible magenta cursor in non-focusable IME window, got $cursorPixels pixels", cursorPixels > 4)
    }
    @Test fun liveModeKeepsKeysVisibleInsteadOfShowingManualCandidateList() {
        show(KeyboardUiState(draftMode = true, liveTranslation = true, draft = "hello", candidates = listOf("translated"),
            settings = com.lazytalk.keyboard.core.model.UserSettings(mode = com.lazytalk.keyboard.core.model.TranslationMode.REMOTE)))
        compose.onNodeWithContentDescription("实时翻译已开启").assertIsDisplayed()
        compose.onNodeWithText("实时").assertDoesNotExist()
        compose.onNodeWithText("q").assertIsDisplayed()
        compose.onNodeWithText("Convert").assertDoesNotExist()
        compose.onNodeWithText("translated").assertDoesNotExist()
    }
    @Test fun catalogWarningStaysInLanguagePickerWhileRealTranslationErrorsRemainVisible() {
        var state by mutableStateOf(KeyboardUiState(draftMode = true, liveTranslation = true, draft = "hello",
            languageError = "Old catalog warning", languages = listOf(Language("en", "English", "English")),
            settings = com.lazytalk.keyboard.core.model.UserSettings(mode = com.lazytalk.keyboard.core.model.TranslationMode.REMOTE)))
        compose.setContent { LazyTalkTheme { KeyboardContent(state, {}, {}, {}, {}, {}, {}) } }
        compose.onNodeWithText("Old catalog warning").assertDoesNotExist()
        compose.onNodeWithText("实时").assertDoesNotExist()
        compose.onNodeWithContentDescription("实时翻译已开启").assertIsDisplayed()
        compose.runOnIdle { state = state.copy(picker = false) }
        compose.onNodeWithText("Old catalog warning").assertIsDisplayed()
        compose.runOnIdle { state = state.copy(picker = null, error = "Current translation failed") }
        compose.onNodeWithText("Current translation failed").assertIsDisplayed()
        compose.onNodeWithContentDescription("重试翻译").assertIsDisplayed()
    }
    @Test fun copiedRecognitionIsOnlyRequestedByTheToolbarClick() {
        var reads = 0
        compose.setContent { LazyTalkTheme { KeyboardContent(KeyboardUiState(), {}, {}, {}, {}, {}, {},
            onRecognizeCopied = { reads++ }) } }
        compose.runOnIdle { assertTrue(reads == 0) }
        compose.onNodeWithContentDescription("打开剪贴板页面").performClick()
        compose.runOnIdle { assertTrue(reads == 1) }
    }
    @Test @Config(qualifiers = "w891dp-h411dp-land")
    fun copiedSentencePreviewKeepsConfirmAndCloseVisibleInLandscape() {
        show(KeyboardUiState(copiedPanelOpen = true, copiedSentences = listOf("Where are you now?", "Awak dekat mana sekarang?")))
        compose.onNodeWithText("Done").assertIsDisplayed()
        compose.onNodeWithText("Detect language and use for replies").assertIsDisplayed()
        compose.onNodeWithText("Awak dekat mana sekarang?").assertIsDisplayed()
        compose.onNodeWithText("q").assertDoesNotExist()
        assertTrue(keyboardHeight() <= 281f)
    }

    @Test fun offlineIbanRemainsSelectableAlongsideConnectionWarning() {
        show(KeyboardUiState(picker = false, languageError = "Backend unavailable", languages = listOf(
            Language("iban", "Iban", "Iban", "offline"))))
        for (key in listOf("i", "b", "a", "n")) compose.onNodeWithText(key).performClick()
        compose.onNodeWithText("Iban · Iban (offline)").assertIsDisplayed()
        compose.onNodeWithText("Backend unavailable").assertIsDisplayed()
        compose.onNodeWithText("Retry connection").assertIsDisplayed()
    }

    @Test fun toolbarOffersSettingsAndClipboardPageWithoutDirectPaste() {
        var settings = 0
        var pasted = 0
        var opened = 0
        compose.setContent {
            LazyTalkTheme {
                KeyboardContent(KeyboardUiState(), {}, {}, {}, {}, { pasted++ }, {},
                    onSettings = { settings++ }, onOpenRecognition = { opened++ })
            }
        }
        compose.onNodeWithContentDescription("Keyboard settings").performClick()
        compose.onNodeWithContentDescription("打开剪贴板页面").performClick()
        compose.runOnIdle { assertTrue(settings == 1 && opened == 1 && pasted == 0) }
        compose.onNodeWithContentDescription("Paste into translation draft").assertDoesNotExist()
        compose.onNodeWithText("识别").assertDoesNotExist()
        assertTrue(keyboardHeight() <= 250f)
    }

    @Test fun normalToolbarShowsTypingLanguageOnRightInsteadOfTranslationTarget() {
        show(KeyboardUiState(target = "ms", languages = listOf(Language("ms", "Malay", "Bahasa Melayu"))))
        compose.onNodeWithContentDescription("Translate").assertIsDisplayed()
        compose.onNodeWithContentDescription("打开剪贴板页面").assertIsDisplayed()
        compose.onNodeWithText("Bahasa Melayu ▾").assertDoesNotExist()
        val language = compose.onNodeWithContentDescription("当前键盘语言：English").fetchSemanticsNode().boundsInRoot
        val settings = compose.onNodeWithContentDescription("Keyboard settings").fetchSemanticsNode().boundsInRoot
        assertTrue(language.left > settings.right)
    }
    @Test fun middleLetterRowIsStaggeredByHalfAKey() {
        show()
        val q = compose.onNodeWithText("q").fetchSemanticsNode().boundsInRoot
        val a = compose.onNodeWithText("a").fetchSemanticsNode().boundsInRoot
        assertTrue(a.left > q.left && a.left - q.left < 30f * compose.density.density)
    }

    @Test fun unavailableLanguagesShowConnectionErrorAndRetryInsteadOfSearchMiss() {
        var retried = false
        compose.setContent {
            LazyTalkTheme {
                KeyboardContent(KeyboardUiState(picker = false, languageError = "Backend offline"),
                    {}, {}, {}, {}, {}, { retried = true })
            }
        }
        compose.onNodeWithText("No matching languages").assertDoesNotExist()
        compose.onNodeWithText("Backend offline").assertIsDisplayed()
        compose.onNodeWithText("Retry language list").performClick()
        compose.runOnIdle { assertTrue(retried) }
    }

    @Test fun ibanIsSearchableWithImeKeysWhenCapabilitiesAreLoaded() {
        show(KeyboardUiState(picker = false, languages = listOf(
            Language("en", "English", "English"), Language("iban", "Iban", "Iban", "beta"))))
        for (key in listOf("i", "b", "a", "n")) compose.onNodeWithText(key).performClick()
        compose.onNodeWithText("Iban · Iban (beta)").assertIsDisplayed()
        compose.onNodeWithText("No matching languages").assertDoesNotExist()
    }

    private fun show(initial: KeyboardUiState = KeyboardUiState()) {
        compose.setContent {
            var state by remember { mutableStateOf(initial) }
            LazyTalkTheme {
                KeyboardContent(state, { event ->
                    state = when (event) {
                        KeyboardAction.ToggleDraft -> state.copy(draftMode = !state.draftMode)
                        is KeyboardAction.Draft -> state.copy(draft = event.text, candidates = emptyList())
                        else -> state
                    }
                }, {}, {}, {}, {}, {})
            }
        }
    }
    private fun keyboardHeight(): Float = with(compose.density) {
        compose.onNodeWithTag("keyboard").fetchSemanticsNode().boundsInRoot.height.toDp().value
    }

    private fun verifyNavigationInsets(left: Int, right: Int, bottom: Int) {
        compose.setContent {
            LazyTalkTheme {
                KeyboardContent(KeyboardUiState(), {}, {}, {}, {}, {}, {},
                    navigationInsets = WindowInsets(left = left.dp, right = right.dp, bottom = bottom.dp))
            }
        }
        val window = compose.onNodeWithTag("keyboardWindow").fetchSemanticsNode().boundsInRoot
        val keys = compose.onNodeWithTag("keyboard").fetchSemanticsNode().boundsInRoot
        with(compose.density) {
            assertTrue(kotlin.math.abs((keys.left - window.left).toDp().value - left) < 1f)
            assertTrue(kotlin.math.abs((window.right - keys.right).toDp().value - right) < 1f)
            assertTrue(kotlin.math.abs((window.bottom - keys.bottom).toDp().value - bottom) < 1f)
        }
        compose.onNodeWithText("Space").assertIsDisplayed()
        compose.onNodeWithText("Enter").assertIsDisplayed()
    }

    @Test fun portraitKeysStayAboveThreeButtonNavigation() = verifyNavigationInsets(0, 0, 48)

    @Test @Config(qualifiers = "w891dp-h411dp-land")
    fun landscapeKeysStayBesideRightNavigation() = verifyNavigationInsets(0, 48, 0)

    @Test @Config(qualifiers = "w891dp-h411dp-land")
    fun reverseLandscapeKeysStayBesideLeftNavigation() = verifyNavigationInsets(48, 0, 0)

    @Test fun spaceLongPressSwitchesWithoutInsertingSpace() {
        var spaces = 0
        var switches = 0
        compose.setContent {
            LazyTalkTheme {
                KeyboardRows(KeyboardUiState(), { if (it == KeyboardAction.Key(" ")) spaces++ },
                    onSwitch = { switches++ })
            }
        }
        compose.onNodeWithText("Space").performTouchInput { longClick() }
        compose.runOnIdle { assertTrue(switches == 1 && spaces == 0) }
        compose.onNodeWithText("Space").performTouchInput { click() }
        compose.runOnIdle { assertTrue(switches == 1 && spaces == 1) }
    }

    @Test fun portraitStartsCompactAndExpandsOnlyWhenRequested() {
        show()
        assertTrue("Ordinary portrait typing must fit in 250dp", keyboardHeight() <= 250f)
        compose.onNodeWithText("Convert").assertDoesNotExist()
        compose.onNodeWithText("q").assertIsDisplayed()
        compose.onNodeWithContentDescription("Hide keyboard").assertDoesNotExist()
        compose.onNodeWithContentDescription("Translate").performClick()
        compose.onNodeWithText("Convert").assertIsDisplayed()
        compose.onNodeWithText("Selection").assertDoesNotExist()
        assertTrue("Two-row portrait translation must fit in 296dp", keyboardHeight() <= 296f)
        compose.onNodeWithContentDescription("Draft options").performClick()
        compose.onNodeWithText("Use selected text").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Exit translation").performClick()
        assertTrue(keyboardHeight() <= 250f)
    }

    @Test @Config(qualifiers = "w891dp-h411dp-land")
    fun landscapeKeepsKeysAndConvertVisibleWithoutStackingPortraitRows() {
        show()
        assertTrue("Ordinary landscape typing must fit in 206dp", keyboardHeight() <= 206f)
        compose.onNodeWithContentDescription("Translate").performClick()
        compose.onNodeWithText("Convert").assertIsDisplayed()
        compose.onNodeWithText("q").assertIsDisplayed()
        compose.onNodeWithContentDescription("Draft options").assertIsDisplayed()
        compose.onNodeWithText("Selection").assertDoesNotExist()
        assertTrue("Landscape draft editing must fit in 240dp", keyboardHeight() <= 240f)
    }

    @Test @Config(qualifiers = "w891dp-h411dp-land")
    fun landscapeResultsReplaceKeysAndEditingRestoresThem() {
        show(KeyboardUiState(draftMode = true, draft = "sample", candidates = listOf("Option one", "Option two", "Option three")))
        compose.onNodeWithText("q").assertDoesNotExist()
        compose.onNodeWithText("Option three").assertIsDisplayed()
        assertTrue(keyboardHeight() <= 281f)
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithText("q").assertIsDisplayed()
        compose.onNodeWithText("Option one").assertDoesNotExist()
    }

    @Test fun portraitResultsReplaceKeysRatherThanStackingAboveThem() {
        show(KeyboardUiState(draftMode = true, draft = "sample", candidates = listOf("Option one", "Option two", "Option three")))
        compose.onNodeWithText("q").assertDoesNotExist()
        compose.onNodeWithText("Option three").assertIsDisplayed()
        assertTrue(keyboardHeight() <= 301f)
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithText("q").assertIsDisplayed()
        assertTrue(keyboardHeight() <= 296f)
    }

    @Test @Config(qualifiers = "w320dp-h480dp-port")
    fun smallPortraitKeepsDraftConvertAndKeysWithinTheAvailableHeight() {
        show(KeyboardUiState(draftMode = true, draft = "A long draft. ".repeat(100)))
        compose.onNodeWithText("Convert").assertIsDisplayed()
        compose.onNodeWithText("q").assertIsDisplayed()
        compose.onNodeWithText("Space").assertIsDisplayed()
        assertTrue(keyboardHeight() <= 251f)
    }

    @Test @Config(qualifiers = "w891dp-h411dp-land")
    fun landscapeErrorDoesNotHideTheDraftKeysOrRetryButton() {
        show(KeyboardUiState(draftMode = true, draft = "A long draft. ".repeat(100), error = "Network unavailable. Try again."))
        compose.onNodeWithText("Retry").assertIsDisplayed()
        compose.onNodeWithText("q").assertIsDisplayed()
        compose.onNodeWithText("Space").assertIsDisplayed()
        assertTrue(keyboardHeight() <= 281f)
    }

    @Test @Config(qualifiers = "w891dp-h411dp-land")
    fun landscapeLanguageSearchHasVisibleListAndKeysSideBySide() {
        show(KeyboardUiState(picker = false, languages = listOf(
            Language("en", "English", "English"), Language("ms", "Malay", "Bahasa Melayu"))))
        compose.onAllNodesWithText("English · English").onFirst().assertIsDisplayed()
        compose.onNodeWithText("q").assertIsDisplayed()
        val search = compose.onNodeWithText("Search Languages").fetchSemanticsNode().boundsInRoot
        val key = compose.onNodeWithText("q").fetchSemanticsNode().boundsInRoot
        assertTrue("Landscape search keys must sit beside the language list", key.left > search.right)
        assertTrue(keyboardHeight() <= 281f)
    }
    @Test @Config(qualifiers="zh-rCN-w411dp-h891dp-port") fun chineseClipboardKeepsEnglishLanguageRecoveryEntry() {
        show(KeyboardUiState(copiedPanelOpen=true,copiedSentences=listOf("Hello there")))
        compose.onNodeWithText("剪贴板").assertIsDisplayed()
        compose.onNodeWithText("粘贴").assertIsDisplayed()
        compose.onNodeWithText("Language").assertIsDisplayed()
        compose.onNodeWithText("Hello there").assertIsDisplayed()
    }
    @Test fun englishLayoutPickerUsesEnglishAndNativeNames() {
        show(KeyboardUiState(layoutPicker=true))
        compose.onNodeWithText("Keyboard languages").assertIsDisplayed()
        compose.onNodeWithText("English · English").assertIsDisplayed()
        compose.onNodeWithText("Language").assertIsDisplayed()
    }
    @Test @Config(qualifiers="zh-rCN-w411dp-h891dp-port") fun chineseTranslationUsesChineseInterfaceWithoutChangingTypingLayout() {
        show(KeyboardUiState(draftMode=true))
        compose.onNodeWithText("输入回复").assertIsDisplayed()
        compose.onNodeWithText("自动检测 ▾").assertIsDisplayed()
        compose.onNodeWithText("q").assertIsDisplayed()
    }
}
