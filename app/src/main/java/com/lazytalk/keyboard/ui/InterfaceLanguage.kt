package com.lazytalk.keyboard.ui

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object InterfaceLanguage {
    fun selected(context: Context): String = context.getSharedPreferences("interface_language",Context.MODE_PRIVATE).getString("language","") ?: ""
    fun wrap(context: Context): Context {
        val tag=selected(context)
        if(tag.isBlank()) return context
        val configuration=Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(tag))
        return context.createConfigurationContext(configuration)
    }
    fun select(activity: Activity, tag: String) {
        require(tag in listOf("","zh","en"))
        activity.getSharedPreferences("interface_language",Context.MODE_PRIVATE).edit().putString("language",tag).apply()
        activity.recreate()
    }
}
