package com.lazytalk.keyboard.feature.keyboard.domain
import com.lazytalk.keyboard.core.common.*
import com.lazytalk.keyboard.core.data.TranslationRepository
import com.lazytalk.keyboard.core.model.*
class TranslateTextUseCase(private val repository: TranslationRepository) {
    suspend operator fun invoke(request: TranslationRequest, secure: Boolean): TranslationResult {
        if (secure) throw AppException(AppError.SECURE_FIELD)
        if (request.text.isBlank() || request.text.length > 4000) throw AppException(AppError.EMPTY_INPUT)
        return repository.translate(request.copy(text = request.text.trim()))
    }
}
