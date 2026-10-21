package com.lazytalk.keyboard.testutil
import com.lazytalk.keyboard.core.data.*
import com.lazytalk.keyboard.core.model.*
import kotlinx.coroutines.flow.MutableStateFlow
class FakeSettings(initial: UserSettings = UserSettings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)
    override suspend fun update(transform: (UserSettings) -> UserSettings) { settings.value = transform(settings.value) }
}
class FakeLanguages : LanguageRepository {
    override val languages = MutableStateFlow(listOf(Language("en", "English", "English"), Language("ms", "Malay", "Bahasa Melayu")))
    override suspend fun refresh() = Unit
    override fun search(languages: List<Language>, query: String) = languages.filter { it.name.contains(query, true) }
    override suspend fun favorite(code: String) = Unit
    override suspend fun used(code: String) = Unit
}
