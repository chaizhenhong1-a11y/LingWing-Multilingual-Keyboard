package com.lazytalk.keyboard.core.model

data class Language(val code: String, val name: String, val nativeName: String, val status: String = "stable")
enum class Tone(val wire: String) { NATURAL("natural"), CASUAL("casual"), POLITE("polite"), SHORT("short") }
enum class TranslationMode { MOCK, REMOTE }
data class TranslationRequest(val text: String, val sourceLanguage: String, val targetLanguage: String, val tone: Tone)
data class TranslationResult(val detectedLanguage: String, val candidates: List<String>)
enum class KeyVibrationStrength(val durationMs: Int) { LIGHT(15), STANDARD(30), STRONG(45) }

data class UserSettings(
    val target: String = "en", val tone: Tone = Tone.NATURAL,
    val favorites: Set<String> = setOf("en", "zh", "ms"), val recent: List<String> = emptyList(),
    val mode: TranslationMode = TranslationMode.MOCK, val baseUrl: String = "", val typingLayoutId: String = "en", val backendToken: String = "", val keyVibration: Boolean = true, val keyVibrationStrength: KeyVibrationStrength = KeyVibrationStrength.STANDARD
)
