package com.lazytalk.keyboard.core.data.language

import com.google.gson.Gson
import com.lazytalk.keyboard.core.data.*
import com.lazytalk.keyboard.core.model.*
import com.lazytalk.keyboard.core.network.*
import com.lazytalk.keyboard.core.network.dto.LanguagesResponseDto
import com.lazytalk.keyboard.core.network.mapper.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LanguageRepositoryImpl(private val settings: SettingsRepository, private val api: ApiClient,
    private val localJson: () -> String, private val effectiveConfig: (UserSettings) -> UserSettings) : LanguageRepository {
    private val state = MutableStateFlow<List<Language>>(emptyList())
    override val languages = state.asStateFlow()
    private val refreshMutex = Mutex()
    private val verified = mutableMapOf<String, List<Language>>()
    override suspend fun refresh() = refreshMutex.withLock {
        val config = effectiveConfig(settings.settings.first())
        val catalog = withContext(Dispatchers.IO) { Gson().fromJson(localJson(), LanguagesResponseDto::class.java).toDomain() }
        if (config.mode == TranslationMode.MOCK) {
            state.value = catalog
        } else {
            // The bundled catalog survives process restarts. It describes choices, not server availability.
            state.value = verified[config.baseUrl]?.map { it.copy(status = "cached") }
                ?: catalog.map { it.copy(status = "offline") }
            val remote = api.languages(config.baseUrl, config.backendToken).toDomain()
            verified[config.baseUrl] = remote
            state.value = remote
        }
    }
    override fun search(languages: List<Language>, query: String) = languages.filter {
        it.code.contains(query, true) || it.name.contains(query, true) || it.nativeName.contains(query, true)
    }
    override suspend fun favorite(code: String) = settings.update {
        it.copy(favorites = if (code in it.favorites) it.favorites - code else it.favorites + code)
    }
    override suspend fun used(code: String) = settings.update { it.copy(recent = (listOf(code) + it.recent).distinct().take(8)) }
}
