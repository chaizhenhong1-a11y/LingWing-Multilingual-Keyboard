package com.lazytalk.keyboard.core.data.translation
import com.lazytalk.keyboard.core.common.*
import com.lazytalk.keyboard.core.data.*
import com.lazytalk.keyboard.core.model.*
import com.lazytalk.keyboard.translation.TranslationService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
class TranslationRepositoryImpl(private val settings: SettingsRepository,
    private val service: (UserSettings) -> TranslationService) : TranslationRepository {
    override suspend fun translate(request: TranslationRequest): TranslationResult = try {
        service(settings.settings.first()).translate(request)
    } catch (e: CancellationException) { throw e }
      catch (e: AppException) { throw e }
      catch (_: Exception) { throw AppException(AppError.UNKNOWN) }
}
