package com.lazytalk.keyboard.feature.settings.presentation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazytalk.keyboard.core.data.*
import com.lazytalk.keyboard.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.lazytalk.keyboard.BuildConfig
import com.lazytalk.keyboard.core.auth.*
import com.lazytalk.keyboard.core.network.ApiClient
class SettingsViewModel(private val repository: SettingsRepository, private val languagesRepo: LanguageRepository,
    private val accounts: AccountRepository, private val api: ApiClient) : ViewModel() {
    val account = accounts.state
    private val authBusyState = MutableStateFlow(false)
    val authBusy = authBusyState.asStateFlow()
    private val authMessageState = MutableStateFlow<String?>(null)
    val authMessage = authMessageState.asStateFlow()
    val settings = repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())
    val languages = languagesRepo.languages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val messageState = MutableStateFlow<String?>(null)
    val message = messageState.asStateFlow()
    init { refresh(); refreshAccount() }
    private fun accountAction(action: suspend () -> Unit) {
        if (authBusyState.value) return
        authBusyState.value = true
        viewModelScope.launch {
            authMessageState.value = null
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (_: androidx.credentials.exceptions.GetCredentialCancellationException) { authMessageState.value = null }
            catch (_: androidx.credentials.exceptions.NoCredentialException) { authMessageState.value = "未找到 Google 账号，请在手机添加账号后重试，或使用邮箱登录。" }
            catch (_: androidx.credentials.exceptions.GetCredentialException) { authMessageState.value = "Google 登录未完成，请检查 Google Play 服务和网络，或使用邮箱登录。" }
            catch (e: Exception) { authMessageState.value = when(e) {
                is AuthFailure -> e.reason
                is com.lazytalk.keyboard.core.common.AppException -> e.error.message
                is IllegalArgumentException -> e.message
                else -> "操作失败，请检查网络后重试。"
            } }
            finally { authBusyState.value = false }
        }
    }
    private suspend fun useCloud() {
        check(BuildConfig.CLOUD_BACKEND_URL.isNotBlank())
        repository.update { it.copy(mode=TranslationMode.REMOTE, baseUrl=BuildConfig.CLOUD_BACKEND_URL, backendToken="") }
    }
    fun signIn(email: String, password: String, register: Boolean) = accountAction {
        accounts.signIn(email, password, register)
        useCloud()
        if (register) { accounts.verifyEmail(); authMessageState.value = "验证邮件已发送，请验证邮箱后点击“我已验证”。" }
        accounts.refreshAccount(api)
        refresh()
    }
    fun refreshAccount() = accountAction { accounts.refreshAccount(api) }
    fun verifyEmail() = accountAction { accounts.verifyEmail(); authMessageState.value = "验证邮件已发送，请检查收件箱或垃圾邮件。" }
    fun resetPassword(email: String) = accountAction { accounts.resetPassword(email); authMessageState.value = "如果该邮箱已注册，你将收到重置密码邮件。" }
    fun googleSignIn(context: android.content.Context) = accountAction {
        val clientId=BuildConfig.GOOGLE_CLIENT_ID.ifBlank { api.authConfig(BuildConfig.CLOUD_BACKEND_URL).googleClientId }
        val googleToken=GoogleAccountSignIn.token(context,clientId)
        accounts.signInGoogle(googleToken)
        useCloud()
        accounts.refreshAccount(api)
        refresh()
    }
    fun signOut(context: android.content.Context) = accountAction {
        accounts.signOut(); GoogleAccountSignIn.clear(context)
        authMessageState.value="已退出，普通键盘输入仍可使用。"
    }
    fun refresh() { viewModelScope.launch {
        try { languagesRepo.refresh(); messageState.value = null }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { messageState.value = "Cannot load languages. Check Backend URL and retry." }
    } }
    fun update(transform: (UserSettings) -> UserSettings) { viewModelScope.launch {
        try { repository.update(transform); messageState.value = "Saved"; refresh() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { messageState.value = "Could not save settings." }
    } }
    fun favorite(code: String) = update { it.copy(favorites = if (code in it.favorites) it.favorites - code else it.favorites + code) }
}
