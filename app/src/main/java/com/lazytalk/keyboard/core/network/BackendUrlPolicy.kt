package com.lazytalk.keyboard.core.network
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.lazytalk.keyboard.BuildConfig
object BackendUrlPolicy {
    fun isValid(value: String): Boolean = value.toHttpUrlOrNull()?.let {
        (it.isHttps || (BuildConfig.DEBUG && it.scheme == "http" && it.host == "127.0.0.1" && it.port == 8787)) &&
            it.username.isEmpty() && it.password.isEmpty() && it.query == null && it.fragment == null
    } == true
}
