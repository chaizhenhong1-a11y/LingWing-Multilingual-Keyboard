package com.lazytalk.keyboard.feature.keyboard.ime

import android.content.ClipboardManager
import android.content.Intent
import android.content.ComponentName
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import com.lazytalk.keyboard.ui.InterfaceLanguage
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.*
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.lazytalk.keyboard.app.LazyTalkApplication
import com.lazytalk.keyboard.feature.keyboard.presentation.*
import com.lazytalk.keyboard.feature.keyboard.components.KeyboardView
import com.lazytalk.keyboard.ui.LazyTalkTheme
import kotlinx.coroutines.*

class LazyTalkInputMethodService : InputMethodService() {
    private lateinit var owner: ImeViewOwner
    private lateinit var model: KeyboardViewModel
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val input = InputConnectionController({ currentInputConnection }, { currentInputEditorInfo })
    private var inputView: ComposeView? = null
    private var displayLanguage by mutableStateOf("")
    private var secure = true
    private var active = false
    private fun copiedPlainText(): String? = try {
        com.lazytalk.keyboard.feature.copiedlanguage.PlainTextClipboardReader(
            getSystemService(ClipboardManager::class.java)).read()
    } catch (_: SecurityException) { null }
    override fun onCreate() {
        super.onCreate()
        owner = ImeViewOwner()
        val container = (application as LazyTalkApplication).container
        model = ViewModelProvider(owner, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = container.keyboardViewModel() as T
        })[KeyboardViewModel::class.java]
        scope.launch { model.effects.collect { effect -> if (active) {
            val applied = try { input.apply(effect) } catch (_: RuntimeException) { false }
            if (!applied && (effect is KeyboardEffect.UpdateLiveTranslation || effect == KeyboardEffect.BeginLiveTranslation))
                model.liveInsertionFailed()
        } } }
    }
    override fun onCreateInputView(): View {
        inputView?.disposeComposition()
        // Compose creates its window recomposer from an ancestor (the IME's parentPanel),
        // so owners on ComposeView alone cannot satisfy its lifecycle lookup.
        window?.window?.decorView?.let(owner::installOn)
        return ComposeView(this).apply {
            owner.installOn(this)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val language=displayLanguage
                val baseConfiguration=LocalConfiguration.current
                val uiContext=remember(language,baseConfiguration) {InterfaceLanguage.wrap(this@LazyTalkInputMethodService)}
                CompositionLocalProvider(LocalContext provides uiContext, LocalConfiguration provides uiContext.resources.configuration) {
                  LazyTalkTheme { KeyboardView(model,
                onHide = { requestHideSelf(0) },
                onSwitch = { getSystemService(InputMethodManager::class.java).showInputMethodPicker() },
                onSettings = {
                    startActivity(Intent().setComponent(ComponentName(packageName, "$packageName.SettingsActivity"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                onSelection = { if (!secure && active) model.importDraft(input.selectedText()) },
                onOpenRecognition = {
                    if (!secure && active) model.openCopiedPanelWithClipboard(copiedPlainText())
                },
                onRecognizeCopied = {
                    if (!secure && active) {
                        model.pasteReplyReference(copiedPlainText())
                    }
                },
                onPaste = {
                    if (!secure && active) {
                        val clip = getSystemService(ClipboardManager::class.java).primaryClip
                        val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.take(4000)?.toString().orEmpty()
                        if (model.state.value.draftMode) model.insertDraftText(text) else model.importDraft(text)
                    }
                }) } } }
            inputView = this
        }
    }
    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        input.resetLiveTranslation()
        secure = SecureFieldDetector.isSecure(attribute.inputType, attribute.imeOptions)
        active = true
        val cls = attribute.inputType and InputType.TYPE_MASK_CLASS
        model.beginSession(secure, cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE,
            InputConnectionController.actionLabel(attribute))
    }
    override fun onWindowShown() { super.onWindowShown(); displayLanguage=InterfaceLanguage.selected(this); owner.show() }
    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        secure = SecureFieldDetector.isSecure(info.inputType, info.imeOptions)
        active = true
        val cls = info.inputType and InputType.TYPE_MASK_CLASS
        model.beginSession(secure, cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE,
            InputConnectionController.actionLabel(info))
    }
    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if(!model.state.value.draftMode && model.state.value.composition.isNotEmpty() && (newSelStart!=candidatesEnd || newSelEnd!=candidatesEnd)) {
            currentInputConnection?.finishComposingText();model.cancelComposition()
        }
    }
    override fun onWindowHidden() { currentInputConnection?.finishComposingText(); active = false; secure = true; input.resetLiveTranslation(); model.endSession(); owner.hide(); super.onWindowHidden() }
    override fun onFinishInput() { currentInputConnection?.finishComposingText(); active = false; secure = true; input.resetLiveTranslation(); model.endSession(); super.onFinishInput() }
    override fun onEvaluateFullscreenMode() = false
    override fun onDestroy() {
        scope.cancel(); inputView?.disposeComposition(); owner.destroy(); super.onDestroy()
    }
}
