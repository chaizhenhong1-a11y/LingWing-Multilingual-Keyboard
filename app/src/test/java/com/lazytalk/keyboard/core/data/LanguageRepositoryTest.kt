package com.lazytalk.keyboard.core.data
import com.lazytalk.keyboard.core.data.language.LanguageRepositoryImpl
import com.lazytalk.keyboard.core.model.*
import com.lazytalk.keyboard.core.network.ApiClient
import com.lazytalk.keyboard.testutil.FakeSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Protocol
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
class LanguageRepositoryTest {
    private val catalog = """{"languages":[{"code":"en","name":"English","nativeName":"English"},{"code":"iban","name":"Iban","nativeName":"Iban"}]}"""

    @Test fun coldOfflineRemoteStillPublishesBundledCatalog() = runTest {
        val api = ApiClient(OkHttpClient.Builder().addInterceptor { throw IOException("offline") }.build())
        repeat(2) { // New repository models a fresh process with no memory cache.
            val repo = LanguageRepositoryImpl(FakeSettings(UserSettings(mode = TranslationMode.REMOTE, baseUrl = "https://example.com")), api, { catalog }, { it })
            try { repo.refresh(); fail("Expected connection failure") } catch (_: com.lazytalk.keyboard.core.common.AppException) { }
            assertEquals("iban", repo.search(repo.languages.first(), "IBAN").single().code)
            assertTrue(repo.languages.first().all { it.status == "offline" })
        }
    }

    @Test fun successfulProviderListSurvivesRefreshFailure() = runTest {
        var online = true
        val api = ApiClient(OkHttpClient.Builder().addInterceptor { chain ->
            if (!online) throw IOException("offline")
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("""{"languages":[{"code":"iban","name":"Iban","nativeName":"Iban","status":"beta"}]}""".toResponseBody("application/json".toMediaType())).build()
        }.build())
        val repo = LanguageRepositoryImpl(FakeSettings(UserSettings(mode = TranslationMode.REMOTE, baseUrl = "https://example.com")), api, { catalog }, { it })
        repo.refresh()
        assertEquals("iban", repo.languages.first().single().code)
        online = false
        try { repo.refresh(); fail("Expected connection failure") } catch (_: com.lazytalk.keyboard.core.common.AppException) { }
        assertEquals("iban", repo.languages.first().single().code)
        assertEquals("cached", repo.languages.first().single().status)
    }

    @Test fun switchingBackendDoesNotReuseAnotherProvidersVerifiedList() = runTest {
        val settings = FakeSettings(UserSettings(mode = TranslationMode.MOCK))
        val api = ApiClient(OkHttpClient.Builder().addInterceptor { throw IOException("offline") }.build())
        val repo = LanguageRepositoryImpl(settings, api, { catalog }, { it })
        repo.refresh()
        settings.update { it.copy(mode = TranslationMode.REMOTE, baseUrl = "https://other.example") }
        try { repo.refresh() } catch (_: com.lazytalk.keyboard.core.common.AppException) { }
        assertEquals(2, repo.languages.first().size)
        assertTrue(repo.languages.first().all { it.status == "offline" })
    }
    @Test fun loadsRegistrySearchesNativeNamesAndPersistsChoices() = runTest {
        val settings = FakeSettings()
        val repository = LanguageRepositoryImpl(settings, ApiClient(), { """{"languages":[{"code":"zh","name":"Chinese","nativeName":"中文","status":"stable"},{"code":"iban","name":"Iban","nativeName":"Iban","status":"beta"}]}""" }, { it })
        repository.refresh()
        assertEquals("zh", repository.search(repository.languages.first(), "中文").single().code)
        assertEquals("iban", repository.search(repository.languages.first(), "IBAN").single().code)
        repository.favorite("iban"); assertTrue("iban" in settings.settings.value.favorites)
        repository.favorite("iban"); assertFalse("iban" in settings.settings.value.favorites)
        repeat(12) { repository.used("lang-$it") }
        repository.used("lang-10")
        assertEquals(8, settings.settings.value.recent.size)
        assertEquals("lang-10", settings.settings.value.recent.first())
        assertEquals(1, settings.settings.value.recent.count { it == "lang-10" })
    }
}
