package com.lazytalk.keyboard.core.auth

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class AuthFailure(val reason: String) : Exception(reason)
class IdentityApi(private val apiKey: String, private val client: OkHttpClient = OkHttpClient.Builder()
    .callTimeout(20, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()) {
    private val gson = Gson()
    private suspend fun request(operation: String, body: Any): JsonObject = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw AuthFailure("登录尚未配置，请安装最新版本。")
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:$operation?key=$apiKey"
        val req = Request.Builder().url(url).post(gson.toJson(body).toRequestBody("application/json".toMediaType())).build()
        execute(req)
    }
    private fun execute(request: Request): JsonObject {
        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.source() ?: throw AuthFailure("登录服务暂时不可用。")
                raw.request(65537)
                if (raw.buffer.size > 65536) throw AuthFailure("登录响应异常，请重试。")
                val result = gson.fromJson(raw.readUtf8(), JsonObject::class.java)
                if (!response.isSuccessful) {
                    val code = result?.getAsJsonObject("error")?.get("message")?.asString.orEmpty()
                    throw AuthFailure(when {
                        code.startsWith("EMAIL_EXISTS") -> "这个邮箱已注册，请直接登录。"
                        code.startsWith("ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL") -> "这个邮箱已使用其他方式注册，请先用原来的方式登录。"
                        code.startsWith("WEAK_PASSWORD") -> "请使用至少 8 位密码。"
                        code.startsWith("INVALID_EMAIL") -> "邮箱格式不正确。"
                        code.startsWith("TOO_MANY_ATTEMPTS") -> "操作太频繁，请稍后重试。"
                        code.startsWith("INVALID_REFRESH_TOKEN") || code.startsWith("TOKEN_EXPIRED") || code.startsWith("USER_DISABLED") || code.startsWith("USER_NOT_FOUND") -> "登录已失效，请重新登录。"
                        else -> "邮箱或密码不正确，或操作暂时不可用。"
                    })
                }
                return result ?: throw AuthFailure("登录响应异常，请重试。")
            }
        } catch (e: AuthFailure) { throw e }
        catch (_: IOException) { throw AuthFailure("网络连接失败，请稍后重试。") }
        catch (_: Exception) { throw AuthFailure("登录服务暂时不可用。") }
    }
    suspend fun signIn(email: String, password: String, register: Boolean) = request(if (register) "signUp" else "signInWithPassword",
        mapOf("email" to email, "password" to password, "returnSecureToken" to true))
    suspend fun google(idToken: String) = request("signInWithIdp", mapOf(
        "postBody" to FormBody.Builder().add("id_token",idToken).add("providerId","google.com").build().let { body ->
            okio.Buffer().also { body.writeTo(it) }.readUtf8()
        }, "requestUri" to "https://holysht.firebaseapp.com", "returnSecureToken" to true, "returnIdpCredential" to false))
    suspend fun refresh(token: String): JsonObject = withContext(Dispatchers.IO) {
        execute(Request.Builder().url("https://securetoken.googleapis.com/v1/token?key=$apiKey")
            .post(FormBody.Builder().add("grant_type", "refresh_token").add("refresh_token", token).build()).build())
    }
    suspend fun lookup(token: String) = request("lookup", mapOf("idToken" to token))
    suspend fun verifyEmail(token: String) { request("sendOobCode", mapOf("requestType" to "VERIFY_EMAIL", "idToken" to token)) }
    suspend fun resetPassword(email: String) { request("sendOobCode", mapOf("requestType" to "PASSWORD_RESET", "email" to email)) }
}
