package com.lazytalk.keyboard.core.network

import com.lazytalk.keyboard.core.common.*
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class CloudAuthorizationTest {
    @Test fun bearerIsSentToHttpsButNeverUsbHttp() = runBlocking {
        val requests=mutableListOf<Request>()
        val client=OkHttpClient.Builder().addInterceptor { chain ->
            requests+=chain.request()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("{\"languages\":[]}".toResponseBody("application/json".toMediaType())).build()
        }.build()
        val api=ApiClient(client)
        api.languages("https://example.com","lt_test_client_access")
        api.languages("http://127.0.0.1:8787","lt_test_client_access")
        assertEquals("Bearer lt_test_client_access",requests[0].header("Authorization"))
        assertNull(requests[1].header("Authorization"))
    }
    @Test fun invalidCloudTokenHasActionableError() = runBlocking {
        val client=OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized")
                .body("{}".toResponseBody("application/json".toMediaType())).build()
        }.build()
        try { ApiClient(client).languages("https://example.com","wrong");fail("Must reject") }
        catch(error: AppException) { assertEquals(AppError.UNAUTHORIZED,error.error) }
    }
}
