package com.lazytalk.keyboard.core.network.mapper

import com.lazytalk.keyboard.core.common.*
import com.lazytalk.keyboard.core.model.*
import com.lazytalk.keyboard.core.network.dto.*

fun TranslationRequest.toDto() = TranslationRequestDto(text, sourceLanguage, targetLanguage, tone.wire)
fun TranslationResponseDto.toDomain(): TranslationResult {
    val language = detectedLanguage?.trim()?.takeIf { it.isNotEmpty() && it.length < 40 }
        ?: throw AppException(AppError.INVALID_RESPONSE)
    val sentences = candidates.orEmpty().mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.distinct().take(3)
    if (sentences.isEmpty() || sentences.any { it.length > 8000 }) throw AppException(AppError.INVALID_RESPONSE)
    return TranslationResult(language, sentences)
}
fun LanguagesResponseDto.toDomain(): List<Language> {
    val result = languages.orEmpty().map { dto ->
        val code = dto.code?.takeIf { it.matches(Regex("[A-Za-z0-9-]{2,35}")) && it != "auto" }
            ?: throw AppException(AppError.INVALID_RESPONSE)
        Language(code, dto.name?.takeIf(String::isNotBlank) ?: throw AppException(AppError.INVALID_RESPONSE),
            dto.nativeName?.takeIf(String::isNotBlank) ?: dto.name, dto.status ?: "beta")
    }.distinctBy { it.code }
    if (result.isEmpty()) throw AppException(AppError.INVALID_RESPONSE)
    return result
}
