package com.lazytalk.keyboard.core.data

import com.lazytalk.keyboard.core.model.*
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun update(transform: (UserSettings) -> UserSettings)
}
interface TranslationRepository { suspend fun translate(request: TranslationRequest): TranslationResult }
interface LanguageRepository {
    val languages: Flow<List<Language>>
    suspend fun refresh()
    fun search(languages: List<Language>, query: String): List<Language>
    suspend fun favorite(code: String)
    suspend fun used(code: String)
}
