package com.lazytalk.keyboard.core.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import com.lazytalk.keyboard.core.data.SettingsRepository
import com.lazytalk.keyboard.core.model.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import java.io.IOException

private val Context.settingsStore by preferencesDataStore("lazytalk_settings")
object SettingsCodec {
    fun vibrationStrength(value: String?) = KeyVibrationStrength.entries.firstOrNull {it.name==value} ?: KeyVibrationStrength.STANDARD
    fun tone(value: String?) = Tone.entries.firstOrNull { it.name == value } ?: Tone.NATURAL
    fun mode(value: String?) = TranslationMode.entries.firstOrNull { it.name == value } ?: TranslationMode.MOCK
    fun recent(value: String?) = value.orEmpty().split(',').filter { it.isNotBlank() }.distinct().take(8)
}
class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {
    constructor(context: Context) : this(context.applicationContext.settingsStore)
    private val target = stringPreferencesKey("target")
    private val tone = stringPreferencesKey("tone")
    private val favorites = stringSetPreferencesKey("favorites")
    private val recent = stringPreferencesKey("recent")
    private val mode = stringPreferencesKey("mode")
    private val typingLayout = stringPreferencesKey("typing_layout")
    private val token = stringPreferencesKey("backend_token")
    private val vibrationStrength = stringPreferencesKey("key_vibration_strength")
    private val vibration = booleanPreferencesKey("key_vibration")
    private val url = stringPreferencesKey("url")
    private fun decode(p: Preferences) = UserSettings(p[target] ?: "en", SettingsCodec.tone(p[tone]),
        p[favorites] ?: setOf("en", "zh", "ms"), SettingsCodec.recent(p[recent]), SettingsCodec.mode(p[mode]), p[url].orEmpty(), TypingLayouts.find(p[typingLayout].orEmpty()).id, p[token].orEmpty(), p[vibration] ?: true, SettingsCodec.vibrationStrength(p[vibrationStrength]))
    override val settings = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map(::decode)
    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        store.edit { p ->
            val s = transform(decode(p))
            p[typingLayout] = TypingLayouts.find(s.typingLayoutId).id
            p[token] = s.backendToken.trim()
            p[vibration] = s.keyVibration
            p[vibrationStrength] = s.keyVibrationStrength.name
            p[target] = s.target; p[tone] = s.tone.name; p[favorites] = s.favorites
            p[recent] = s.recent.distinct().take(8).joinToString(","); p[mode] = s.mode.name; p[url] = s.baseUrl.trim()
        }
    }
}
