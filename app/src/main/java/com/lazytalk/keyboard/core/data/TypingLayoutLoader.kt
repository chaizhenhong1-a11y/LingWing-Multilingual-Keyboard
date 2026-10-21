package com.lazytalk.keyboard.core.data

import android.content.res.AssetManager
import com.google.gson.JsonParser
import com.lazytalk.keyboard.core.model.*
import java.util.Locale

object TypingLayoutLoader {
    fun load(assets: AssetManager): List<TypingLayout> = assets.open("typing-layouts.json").bufferedReader().use { reader ->
        JsonParser.parseReader(reader).asJsonArray.map { node ->
            val o = node.asJsonObject
            fun rows(key: String) = o[key].asJsonArray.map { row -> row.asJsonArray.map { it.asString } }
            val tag = o["tag"].asString
            val locale = Locale.forLanguageTag(tag)
            val native = if (tag == "iban") "Iban" else locale.getDisplayName(locale).ifBlank { tag }
            val lower = rows("tokenRows"); val upper = rows("shiftedTokenRows")
            val popup = o["popups"].asJsonObject.entrySet().associate { (k,v) -> k to v.asJsonArray.map { it.asString } }
            TypingLayout(o["id"].asString, native, lower.map { it.joinToString("") }, upper.map { it.joinToString("") },
                stagger = lower.size == 3 && lower[0].size == 10 && lower[1].size == 9,
                tokenRows = lower, shiftedTokenRows = upper, popups = popup, variant = o["variant"].asString, tag = tag)
        }
    }
}
