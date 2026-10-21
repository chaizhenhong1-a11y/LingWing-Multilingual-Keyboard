package com.lazytalk.keyboard.feature.settings

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.lazytalk.keyboard.core.auth.AccountState
import com.lazytalk.keyboard.feature.settings.presentation.AccountContent
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],qualifiers="zh-rCN-w411dp-h891dp-port")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccountScreenTest {
    @get:Rule val compose=createComposeRule()
    private lateinit var host: android.view.View
    private fun show(account: AccountState=AccountState(ready=true), onEmail:(String,String,Boolean)->Unit={_,_,_->}, onGoogle:()->Unit={}) {
        compose.setContent { host=LocalView.current;MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF86D4BF))) {
            AccountContent(account,false,null,onEmail,onGoogle,{},{},{},{},{})
        } }
    }
    private fun preview(name:String) {
        val folder=File(System.getProperty("java.io.tmpdir"),"account-preview").apply {mkdirs()}
        compose.runOnIdle {
            val bitmap=Bitmap.createBitmap(host.width,host.height,Bitmap.Config.ARGB_8888)
            host.draw(android.graphics.Canvas(bitmap))
            File(folder,name).outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
    @Test fun loginHasSeparateGoogleEntryAndWorkingEmailSubmission() {
        var google=false;var submitted:Triple<String,String,Boolean>?=null
        show(onEmail={e,p,r->submitted=Triple(e,p,r)},onGoogle={google=true})
        compose.onNodeWithText("使用 Google 账号继续").assertIsDisplayed().performClick();assertTrue(google)
        preview("login-dark.png")
        compose.onNodeWithText("邮箱地址").performTextInput("test@example.com")
        compose.onNodeWithText("密码",useUnmergedTree=false).performTextInput("test-password")
        compose.onNodeWithText("登录",substring=false).performScrollTo().performClick()
        assertEquals(Triple("test@example.com","test-password",false),submitted)
    }
    @Test fun ownerAccountShowsUnlimitedWithoutDailyRemainingNumber() {
        show(AccountState(email="businessstanley8@gmail.com",verified=true,ready=true,unlimited=true,limit=null,remaining=null))
        compose.onNodeWithText("免费 · 不限翻译次数").assertIsDisplayed()
        compose.onNodeWithText("今日剩余",substring=true).assertDoesNotExist()
        preview("account-unlimited-dark.png")
    }
    @Test @Config(qualifiers="zh-rCN-w800dp-h360dp-land") fun smallLandscapeCanReachRegistrationAndSubmitControls() {
        show()
        compose.onNodeWithText("还没有账号？免费注册").performScrollTo().performClick()
        compose.onNodeWithText("创建你的账号").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("创建账号",substring=false).performScrollTo().assertExists()
        compose.onNodeWithText("至少 8 位字符").assertExists()
    }
    @Test @Config(qualifiers="en-rUS-w411dp-h891dp-port") fun englishInterfaceUsesEnglishAccountLabels() {
        show()
        compose.onNodeWithText("Continue with Google").assertIsDisplayed()
        compose.onNodeWithText("Email address").assertIsDisplayed()
        compose.onNodeWithText("Sign in",substring=false).assertExists()
    }
}
