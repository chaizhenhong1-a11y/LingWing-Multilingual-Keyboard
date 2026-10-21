package com.lazytalk.keyboard.core.auth

import com.lazytalk.keyboard.BuildConfig
import com.lazytalk.keyboard.core.common.*
import com.lazytalk.keyboard.core.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class AccountState(val email: String = "", val verified: Boolean = false, val ready: Boolean = false,
    val remaining: Int? = null, val limit: Int? = 50, val unlimited: Boolean = false)

class AccountRepository(private val identity: IdentityApi, private val store: SessionStore,
    private val clock: () -> Long = System::currentTimeMillis) {
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(AccountState())
    val state = mutable.asStateFlow()
    private var idToken = ""
    private var expiresAt = 0L
    private var restored = false
    private var refreshToken: String? = null

    private suspend fun restore() { if (!restored) { refreshToken = withContext(Dispatchers.IO) { store.read() }; restored = true } }
    private suspend fun save(value: String?) { withContext(Dispatchers.IO) { store.write(value) }; refreshToken = value }
    private suspend fun tokenLocked(force: Boolean): String {
        restore()
        if (!force && idToken.isNotEmpty() && expiresAt > clock() + 60000) return idToken
        val refresh = refreshToken ?: throw AppException(AppError.UNAUTHORIZED)
        val response = try { identity.refresh(refresh) }
            catch(e: AuthFailure) {
                if(e.reason=="登录已失效，请重新登录。") throw AppException(AppError.UNAUTHORIZED)
                throw e
            }
        save(response.get("refresh_token").asString)
        idToken = response.get("id_token").asString
        expiresAt = clock() + response.get("expires_in").asLong * 1000
        return idToken
    }
    suspend fun token(force: Boolean = false): String = mutex.withLock { tokenLocked(force) }
    suspend fun signIn(email: String, password: String, register: Boolean) = mutex.withLock {
        require(email.contains('@') && password.length >= 8) { "请输入有效邮箱和至少 8 位密码。" }
        val response = identity.signIn(email.trim(), password, register)
        acceptSession(response)
    }
    suspend fun signInGoogle(googleToken: String) = mutex.withLock {
        acceptSession(identity.google(googleToken))
    }
    private suspend fun acceptSession(response: com.google.gson.JsonObject) {
        save(response.get("refreshToken").asString)
        restored = true
        idToken = response.get("idToken").asString
        expiresAt = clock() + response.get("expiresIn").asLong * 1000
        mutable.value = AccountState(email = response.get("email").asString, ready = true)
    }
    suspend fun refreshAccount(api: ApiClient) {
        mutex.withLock {
            restore()
            if (refreshToken == null) { mutable.value = AccountState(ready = true); return }
            val fresh = tokenLocked(true)
            val user = identity.lookup(fresh).getAsJsonArray("users")[0].asJsonObject
            mutable.value = mutable.value.copy(email = user.get("email").asString,
                verified = user.get("emailVerified")?.asBoolean == true, ready = true)
        }
        if (BuildConfig.CLOUD_BACKEND_URL.isNotBlank()) {
            val quota = api.account(BuildConfig.CLOUD_BACKEND_URL)
            mutex.withLock { if (refreshToken != null) mutable.value = mutable.value.copy(remaining = quota.remaining, limit = quota.limit, unlimited = quota.unlimited) }
        }
    }
    suspend fun verifyEmail() = mutex.withLock { identity.verifyEmail(tokenLocked(false)) }
    suspend fun resetPassword(email: String) { require(email.contains('@')) { "请先填写邮箱。" }; identity.resetPassword(email.trim()) }
    suspend fun signOut() = mutex.withLock {
        save(null); restored = true; idToken = ""; expiresAt = 0
        mutable.value = AccountState(ready = true)
    }
    fun owns(base: String): Boolean = BuildConfig.CLOUD_BACKEND_URL.isNotBlank() && base.trimEnd('/') == BuildConfig.CLOUD_BACKEND_URL.trimEnd('/')
}
