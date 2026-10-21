package com.lazytalk.keyboard.feature.keyboard.domain
data class LanguagePair(val source: String, val target: String)
class SwapLanguagesUseCase {
    operator fun invoke(source: String, target: String, detected: String?): LanguagePair? {
        val resolved = if (source == "auto") detected else source
        return resolved?.takeIf { it != "auto" && it.isNotBlank() }?.let { LanguagePair(target, it) }
    }
}
