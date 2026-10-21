package com.lazytalk.keyboard.app
import android.app.Application
import com.lazytalk.keyboard.app.di.AppContainer
class LazyTalkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.lazytalk.keyboard.core.model.TypingLayouts.install(com.lazytalk.keyboard.core.data.TypingLayoutLoader.load(assets))
    }
    val container by lazy { AppContainer(this) }
}
