package com.lazytalk.keyboard.core.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.lazytalk.keyboard.BuildConfig

object GoogleAccountSignIn {
    suspend fun token(context: Context, clientId: String = BuildConfig.GOOGLE_CLIENT_ID): String {
        if(!clientId.matches(Regex("[A-Za-z0-9_-]+\\.apps\\.googleusercontent\\.com"))) throw AuthFailure("Google 登录配置尚未完成，暂时可使用邮箱登录。")
        val option=GetSignInWithGoogleOption.Builder(clientId).build()
        val result=CredentialManager.create(context).getCredential(context,
            GetCredentialRequest.Builder().addCredentialOption(option).build())
        val credential=result.credential
        if(credential !is CustomCredential || credential.type!=GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
            throw AuthFailure("未取得 Google 登录凭证，请重试。")
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
    suspend fun clear(context: Context) {
        try { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
        catch(e: kotlinx.coroutines.CancellationException) {throw e}
        catch(_: Exception) { /* Local app session has already been removed. */ }
    }
}
