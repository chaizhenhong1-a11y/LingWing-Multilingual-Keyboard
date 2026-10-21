package com.lazytalk.keyboard.translation.remote
import com.lazytalk.keyboard.core.model.*
import com.lazytalk.keyboard.core.network.ApiClient
import com.lazytalk.keyboard.core.network.mapper.*
import com.lazytalk.keyboard.translation.TranslationService
class RemoteTranslationService(private val api: ApiClient, private val baseUrl: String, private val token: String = "") : TranslationService {
    override suspend fun translate(request: TranslationRequest) = api.translate(baseUrl, request.toDto(), token).toDomain()
}
