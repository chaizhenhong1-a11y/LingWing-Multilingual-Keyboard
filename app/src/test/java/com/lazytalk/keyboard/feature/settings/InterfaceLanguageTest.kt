package com.lazytalk.keyboard.feature.settings
import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.lazytalk.keyboard.ui.InterfaceLanguage
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],qualifiers="en-rUS")
class InterfaceLanguageTest {
    @Test fun defaultFollowsSystemAndSavedOverrideChangesResourcesOnly() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val preferences=context.getSharedPreferences("interface_language",Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        assertEquals("",InterfaceLanguage.selected(context))
        assertSame(context,InterfaceLanguage.wrap(context))
        val original=Configuration(context.resources.configuration)
        try {
            preferences.edit().putString("language","zh").commit()
            assertEquals("zh",InterfaceLanguage.wrap(context).resources.configuration.locales[0].language)
            assertEquals(original,context.resources.configuration)
            preferences.edit().putString("language","").commit()
            assertSame(context,InterfaceLanguage.wrap(context))
        } finally {preferences.edit().clear().commit()}
    }
}
