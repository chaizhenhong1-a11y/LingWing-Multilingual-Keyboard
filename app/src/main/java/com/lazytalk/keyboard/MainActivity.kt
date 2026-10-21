package com.lazytalk.keyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lazytalk.keyboard.app.LazyTalkApplication
import com.lazytalk.keyboard.feature.settings.presentation.*
import com.lazytalk.keyboard.ui.LazyTalkTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(base: android.content.Context) { super.attachBaseContext(com.lazytalk.keyboard.ui.InterfaceLanguage.wrap(base)) }
    private val enabled = mutableStateOf(false)
    private val selected = mutableStateOf(false)
    override fun onResume() {
        super.onResume()
        enabled.value=getSystemService(InputMethodManager::class.java).enabledInputMethodList.any {it.packageName==packageName}
        selected.value=android.content.ComponentName.unflattenFromString(Settings.Secure.getString(contentResolver,Settings.Secure.DEFAULT_INPUT_METHOD).orEmpty())?.packageName==packageName
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as LazyTalkApplication).container
        val model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(container.settings, container.languages, container.account, container.api) as T
        })[SettingsViewModel::class.java]
        setContent { LazyTalkTheme {
            Surface(Modifier.fillMaxSize()) {
                Box(Modifier.safeDrawingPadding()) {
                    SettingsScreen(model, {finish()}, if(intent.getBooleanExtra("interfaceLanguage",false)) "interface" else "home",
                        keyboardEnabled=enabled.value, keyboardSelected=selected.value,
                        onEnableKeyboard={startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))},
                        onSelectKeyboard={getSystemService(InputMethodManager::class.java).showInputMethodPicker()})
                }
            }
        } }
    }
}
