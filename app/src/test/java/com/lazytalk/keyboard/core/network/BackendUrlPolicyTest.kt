package com.lazytalk.keyboard.core.network

import org.junit.Assert.*
import org.junit.Test

class BackendUrlPolicyTest {
    @Test fun usbDevelopmentIsRestrictedToExactLoopbackAndPort() {
        assertTrue(BackendUrlPolicy.isValid("http://127.0.0.1:8787"))
        assertTrue(BackendUrlPolicy.isValid("https://example.com/api"))
        listOf("http://192.168.1.2:8787", "http://127.0.0.1:80", "http://localhost:8787",
            "http://127.0.0.1:8787@evil.com", "http://user:pass@127.0.0.1:8787", "http://127.0.0.1:8787?key=value")
            .forEach { assertFalse(it, BackendUrlPolicy.isValid(it)) }
    }
}
