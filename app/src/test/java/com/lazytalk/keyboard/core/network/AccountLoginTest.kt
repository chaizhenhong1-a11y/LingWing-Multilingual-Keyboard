package com.lazytalk.keyboard.core.network

import com.lazytalk.keyboard.core.auth.*
import com.lazytalk.keyboard.core.common.*
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class AccountLoginTest {
    @Test fun unlimitedQuotaHasNoSyntheticNumericLimit() = runBlocking {
        val http=OkHttpClient.Builder().addInterceptor {chain -> response(chain.request(),200,
            """{"limit":null,"remaining":null,"used":5001,"unlimited":true,"plan":"owner_free"}""")}.build()
        val quota=ApiClient(http).account("https://example.com")
        assertTrue(quota.unlimited);assertNull(quota.limit);assertNull(quota.remaining);assertEquals("owner_free",quota.plan)
    }
    private fun response(request: Request, code: Int, body: String) = Response.Builder().request(request)
        .protocol(Protocol.HTTP_1_1).code(code).message("test").body(body.toResponseBody("application/json".toMediaType())).build()
    @Test fun refreshTokenRestoresSessionAndLogoutCannotReuseIt() = runBlocking {
        class Store : SessionStore { var value: String?=null; override fun read()=value;override fun write(value:String?) {this.value=value} }
        val store=Store()
        var refreshes=0
        val http=OkHttpClient.Builder().addInterceptor { chain ->
            val request=chain.request()
            response(request,200,if(request.url.host=="securetoken.googleapis.com") {
                refreshes++;"""{"id_token":"renewed","refresh_token":"rotated-refresh","expires_in":"3600"}"""
            } else """{"email":"test@example.com","idToken":"first","refreshToken":"initial-refresh","expiresIn":"3600"}""")
        }.build()
        val identity=IdentityApi("public-test-api-key",http)
        val first=AccountRepository(identity,store)
        first.signIn("test@example.com","password123",false)
        assertEquals("first",first.token());assertEquals(0,refreshes)
        assertEquals("initial-refresh",store.value)
        val restored=AccountRepository(identity,store)
        assertEquals("renewed",restored.token());assertEquals(1,refreshes)
        assertEquals("rotated-refresh",store.value)
        restored.signOut();assertNull(store.value)
        try {restored.token();fail("Signed-out session must not recover")}
        catch(e:AppException){assertEquals(AppError.UNAUTHORIZED,e.error)}
        assertEquals(1,refreshes)
    }
    @Test fun expiredAccountJwtIsRefreshedOnceAndNeverSentToAnotherHost() = runBlocking {
        val seen=mutableListOf<Request>()
        val refreshFlags=mutableListOf<Boolean>()
        val http=OkHttpClient.Builder().addInterceptor { chain ->
            val req=chain.request();seen+=req
            response(req,if(req.header("Authorization")=="Bearer expired")401 else 200,"""{"languages":[]}""")
        }.build()
        val api=ApiClient(http,credentials={base,force ->
            if(base=="https://trusted.example") {refreshFlags+=force;if(force)"new-token" else "expired"} else null
        })
        api.languages("https://trusted.example")
        api.languages("https://other.example")
        assertEquals(listOf(false,true),refreshFlags)
        assertEquals("Bearer new-token",seen[1].header("Authorization"))
        assertNull(seen[2].header("Authorization"))
    }
    @Test fun exhaustedDailyQuotaHasSpecificUserMessage() = runBlocking {
        val http=OkHttpClient.Builder().addInterceptor {chain -> response(chain.request(),429,"""{"error":"daily_quota_exhausted"}""")}.build()
        try {ApiClient(http).languages("https://example.com");fail("Quota must stop request")}
        catch(e:AppException){assertEquals(AppError.DAILY_QUOTA,e.error)}
    }
}
