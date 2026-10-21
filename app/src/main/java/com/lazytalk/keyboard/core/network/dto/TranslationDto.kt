package com.lazytalk.keyboard.core.network.dto

data class TranslationRequestDto(val text: String, val sourceLanguage: String, val targetLanguage: String, val tone: String)
data class TranslationResponseDto(val detectedLanguage: String?, val candidates: List<String?>?)
data class LanguageDto(val code: String?, val name: String?, val nativeName: String?, val status: String?)
data class LanguagesResponseDto(val languages: List<LanguageDto>?)
