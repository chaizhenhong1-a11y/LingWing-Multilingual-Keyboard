package com.lazytalk.keyboard.feature.keyboard.components
import android.graphics.Bitmap
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lazytalk.keyboard.feature.keyboard.presentation.*
import com.lazytalk.keyboard.ui.LazyTalkTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],qualifiers="en-rUS-w411dp-h891dp-port")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KeySpacingTest {
    @get:Rule val compose=createComposeRule()
    private fun check(draft: Boolean) {
        lateinit var view:android.view.View
        var typed=""
        compose.setContent {view=LocalView.current;LazyTalkTheme {
            KeyboardContent(KeyboardUiState(draftMode=draft),{if(it is KeyboardAction.Key) typed+=it.text},
                {},{},{},{},{},navigationInsets=WindowInsets(0,0,0,0))
        }}
        val target=compose.onNodeWithTag("keyTarget:q",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        val cap=compose.onNodeWithTag("keyCap:q",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        val next=compose.onNodeWithTag("keyCap:w",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        val lower=compose.onNodeWithTag("keyCap:a",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        assertTrue(target.width>cap.width)
        assertTrue(target.height>cap.height)
        assertTrue(next.left-cap.right>=4f)
        assertTrue(lower.top-cap.bottom>=5f)
        compose.onNodeWithTag("keyTarget:q",useUnmergedTree=true).performTouchInput {click(androidx.compose.ui.geometry.Offset(1f,center.y))}
        assertEquals("q",typed)
        compose.onNodeWithText("Space").assertIsDisplayed()
        compose.runOnIdle {
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            val folder=File(System.getProperty("java.io.tmpdir"),"key-spacing").apply {mkdirs()}
            File(folder,if(draft) "landscape.png" else "portrait.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
    @Test fun portraitKeepsFullTouchCellsAroundSeparatedKeycaps()=check(false)
    @Test @Config(qualifiers="en-rUS-w891dp-h411dp-land") fun landscapeTranslationKeepsKeysAndSpacing()=check(true)
}
