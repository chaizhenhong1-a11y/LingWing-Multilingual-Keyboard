package com.lazytalk.keyboard.translation
import com.lazytalk.keyboard.core.model.*
interface TranslationService { suspend fun translate(request: TranslationRequest): TranslationResult }
