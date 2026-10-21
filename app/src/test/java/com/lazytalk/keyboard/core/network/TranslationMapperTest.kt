package com.lazytalk.keyboard.core.network
import com.google.gson.Gson
import com.lazytalk.keyboard.core.common.AppException
import com.lazytalk.keyboard.core.model.*
import com.lazytalk.keyboard.core.network.dto.*
import com.lazytalk.keyboard.core.network.mapper.*
import org.junit.Assert.*
import org.junit.Test
class TranslationMapperTest {
    private val gson = Gson()
    @Test fun serializesContractAndEveryTone() {
        Tone.entries.forEach { tone ->
            val tree = gson.toJsonTree(TranslationRequest("hello", "auto", "ms", tone).toDto()).asJsonObject
            assertEquals("auto", tree["sourceLanguage"].asString)
            assertEquals("ms", tree["targetLanguage"].asString)
            assertEquals(tone.name.lowercase(), tree["tone"].asString)
        }
    }
    @Test fun parsesDetectedLanguageAndBoundsCandidates() {
        val dto = gson.fromJson("""{"detectedLanguage":"ms","candidates":["  你好 ",null,"你好","二","三","四"]}""", TranslationResponseDto::class.java)
        assertEquals(TranslationResult("ms", listOf("你好", "二", "三")), dto.toDomain())
    }
    @Test(expected = AppException::class) fun rejectsEmptyResponse() { TranslationResponseDto("en", emptyList()).toDomain() }
    @Test(expected = AppException::class) fun rejectsMissingDetection() { TranslationResponseDto(null, listOf("hi")).toDomain() }
    @Test fun supportsNewLanguageWithoutCoreChange() {
        val dto = gson.fromJson("""{"languages":[{"code":"new-Latn","name":"New","nativeName":"New","status":"beta"}]}""", LanguagesResponseDto::class.java)
        assertEquals("new-Latn", dto.toDomain().single().code)
    }
    @Test fun requiresSecureUrl() {
        val api = ApiClient()
        assertTrue(api.validBaseUrl("https://example.com/api/"))
        assertFalse(api.validBaseUrl("http://example.com"))
        assertFalse(api.validBaseUrl("https://user:secret@example.com"))
        assertFalse(api.validBaseUrl("https://example.com?key=secret"))
    }
}
