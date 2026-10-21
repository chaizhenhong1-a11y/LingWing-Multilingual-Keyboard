package com.lazytalk.keyboard.feature.keyboard.ime

import android.widget.LinearLayout
import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class ImeWindowLifecycleTest {
    @Test fun unownedWindowReproducesTheReportedComposeCrash() {
        val root = LinearLayout(RuntimeEnvironment.getApplication())
        val error = assertThrows(IllegalStateException::class.java) { root.createLifecycleAwareWindowRecomposer() }
        assertTrue(error.message.orEmpty().contains("ViewTreeLifecycleOwner"))
    }
    @Test fun keyboardWindowAncestorsHaveOwnersBeforeComposeAttaches() {
        val controller = Robolectric.buildService(LazyTalkInputMethodService::class.java).create()
        val service = controller.get()
        try {
            val keyboard = service.onCreateInputView()
            val decor = checkNotNull(service.window?.window?.decorView)
            val owner = checkNotNull(keyboard.findViewTreeLifecycleOwner())
            assertSame("Compose's window recomposer must find the lifecycle on the window root", owner, decor.findViewTreeLifecycleOwner())
            assertSame(keyboard.findViewTreeSavedStateRegistryOwner(), decor.findViewTreeSavedStateRegistryOwner())
            assertSame(keyboard.findViewTreeViewModelStoreOwner(), decor.findViewTreeViewModelStoreOwner())
        } finally { controller.destroy() }
    }
}
