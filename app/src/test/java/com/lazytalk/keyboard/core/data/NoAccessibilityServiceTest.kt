package com.lazytalk.keyboard.core.data

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NoAccessibilityServiceTest {
    @Test fun packageHasNoAccessibilityServiceAndRetainsTheKeyboard() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(context.packageManager.queryIntentServices(Intent(AccessibilityService.SERVICE_INTERFACE)
            .setPackage(context.packageName), 0).isEmpty())
        val services = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SERVICES).services.orEmpty()
        assertTrue(services.any { it.permission == "android.permission.BIND_INPUT_METHOD" })
        assertTrue(services.none { it.permission == "android.permission.BIND_ACCESSIBILITY_SERVICE" })
        services.filter { it.permission != "android.permission.BIND_INPUT_METHOD" }.forEach {
            // Google's SDK exports its permission-protected revocation notification receiver service.
            if (it.name == "com.google.android.gms.auth.api.signin.RevocationBoundService")
                assertEquals("com.google.android.gms.auth.api.signin.permission.REVOCATION_NOTIFICATION", it.permission)
            else assertFalse(it.exported)
        }
    }
}
