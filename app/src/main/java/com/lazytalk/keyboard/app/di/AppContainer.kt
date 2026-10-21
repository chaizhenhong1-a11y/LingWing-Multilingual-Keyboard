package com.lazytalk.keyboard.app.di

import android.content.Context
import com.lazytalk.keyboard.BuildConfig
import com.lazytalk.keyboard.core.data.*
import com.lazytalk.keyboard.core.data.settings.DataStoreSettingsRepository
import com.lazytalk.keyboard.core.data.translation.TranslationRepositoryImpl
import com.lazytalk.keyboard.core.data.language.LanguageRepositoryImpl
import com.lazytalk.keyboard.core.model.*
import com.lazytalk.keyboard.core.network.ApiClient
import com.lazytalk.keyboard.feature.keyboard.domain.TranslateTextUseCase
import com.lazytalk.keyboard.feature.keyboard.presentation.KeyboardViewModel
import com.lazytalk.keyboard.translation.mock.MockTranslationService
import com.lazytalk.keyboard.translation.remote.RemoteTranslationService

class AppContainer(context: Context) {
    val settings: SettingsRepository = DataStoreSettingsRepository(context)
    val account by lazy { com.lazytalk.keyboard.core.auth.AccountRepository(
        com.lazytalk.keyboard.core.auth.IdentityApi(BuildConfig.AUTH_API_KEY),
        com.lazytalk.keyboard.core.auth.KeystoreSessionStore(context)) }
    val api by lazy { ApiClient(credentials = { base, force -> if (account.owns(base)) account.token(force) else null }) }
    private val mock by lazy { MockTranslationService() }
    // Release ignores persisted development mode and URLs. A production URL must be set at build time (not a secret).
    fun effectiveConfig(s: UserSettings) = if (BuildConfig.DEBUG) s
        else s.copy(mode = TranslationMode.REMOTE, baseUrl = BuildConfig.CLOUD_BACKEND_URL.ifBlank { BuildConfig.BACKEND_URL }, backendToken = "")
    val translation: TranslationRepository = TranslationRepositoryImpl(settings) {
        val config = effectiveConfig(it)
        when (config.mode) {
            TranslationMode.MOCK -> mock
            TranslationMode.REMOTE -> RemoteTranslationService(api, config.baseUrl, config.backendToken)
        }
    }
    val languages: LanguageRepository = LanguageRepositoryImpl(settings, api,
        { context.assets.open("languages.json").bufferedReader().use { it.readText() } }, ::effectiveConfig)
    private val typing by lazy { OfflineTypingSuggestions { name -> context.assets.open(name).bufferedReader().use { it.readText() } } }
    fun keyboardViewModel() = KeyboardViewModel(settings, languages, TranslateTextUseCase(translation),
        com.lazytalk.keyboard.feature.copiedlanguage.LocalSentenceLanguageDetector(), typing)
}
