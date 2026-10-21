package com.lazytalk.keyboard.core.network

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.lazytalk.keyboard.core.common.*
import com.lazytalk.keyboard.core.network.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ApiClient(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS)
    .callTimeout(35, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build(),
    private val gson: Gson = Gson(),
    private val credentials: (suspend (String, Boolean) -> String?)? = null) {
    fun validBaseUrl(value: String): Boolean = BackendUrlPolicy.isValid(value)
    private suspend fun <T> call(base: String, path: String, body: Any?, type: Class<T>, token: String = "", refresh: Boolean = false): T = withContext(Dispatchers.IO) {
        if (!validBaseUrl(base)) throw AppException(AppError.INVALID_CONFIGURATION)
        val url = (base.trimEnd('/') + "/" + path).toHttpUrlOrNull() ?: throw AppException(AppError.INVALID_CONFIGURATION)
        val accountToken = if (url.isHttps && path != "auth-config") credentials?.invoke(base, refresh) else null
        val request = Request.Builder().url(url).header("Accept", "application/json").apply {
            val bearer = accountToken ?: token
            if (url.isHttps && bearer.isNotBlank()) header("Authorization", "Bearer $bearer")
            if (body != null) post(gson.toJson(body).toRequestBody("application/json; charset=utf-8".toMediaType()))
        }.build()
        try {
            val response = suspendCancellableCoroutine<Response> { continuation ->
                val call = client.newCall(request)
                continuation.invokeOnCancellation { call.cancel() }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(e) }
                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response) { _, r, _ -> r.close() }
                    }
                })
            }
            response.use {
                if (it.code == 401 && accountToken != null && !refresh) {
                    it.close()
                    return@withContext call(base, path, body, type, token, true)
                }
                if (it.code == 401) throw AppException(AppError.UNAUTHORIZED)
                if (it.code == 403) throw AppException(AppError.EMAIL_UNVERIFIED)
                if (it.code == 429) {
                    val source = it.body?.source()
                    source?.request(2049)
                    val error = if (source != null && source.buffer.size <= 2048) source.readUtf8() else ""
                    throw AppException(if (error.contains("daily_quota_exhausted")) AppError.DAILY_QUOTA
                        else if (error.contains("service_daily_limit")) AppError.SERVICE_QUOTA else AppError.RATE_LIMITED)
                }
                if (it.code == 400) throw AppException(AppError.TEXT_LIMIT)
                if (it.code == 422) throw AppException(AppError.UNSUPPORTED_LANGUAGE)
                if (!it.isSuccessful) throw AppException(AppError.SERVER)
                val source = it.body?.source() ?: throw AppException(AppError.INVALID_RESPONSE)
                source.request(262145)
                if (source.buffer.size > 262144) throw AppException(AppError.INVALID_RESPONSE)
                gson.fromJson(source.readUtf8(), type) ?: throw AppException(AppError.INVALID_RESPONSE)
            }
        } catch (_: SocketTimeoutException) { throw AppException(AppError.TIMEOUT) }
          catch (_: InterruptedIOException) { throw AppException(AppError.TIMEOUT) }
          catch (_: IOException) { throw AppException(AppError.NETWORK) }
          catch (_: JsonParseException) { throw AppException(AppError.INVALID_RESPONSE) }
    }
    suspend fun translate(base: String, request: TranslationRequestDto, token: String = "") = call(base, "translate", request, TranslationResponseDto::class.java, token)
    suspend fun languages(base: String, token: String = "") = call(base, "languages", null, LanguagesResponseDto::class.java, token)
    suspend fun account(base: String) = call(base, "account", null, AccountQuotaDto::class.java)
    suspend fun authConfig(base: String) = call(base,"auth-config",null,AuthConfigDto::class.java)
}
data class AccountQuotaDto(val limit: Int? = 50, val remaining: Int? = 0, val used: Int = 0,
    val unlimited: Boolean = false, val plan: String = "free")
data class AuthConfigDto(val googleClientId: String = "")
