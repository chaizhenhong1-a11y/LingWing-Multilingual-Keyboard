package com.lazytalk.keyboard.feature.keyboard.domain

import java.text.Normalizer

object TelexInput {
    private val tones=mapOf('s' to '\u0301','f' to '\u0300','r' to '\u0309','x' to '\u0303','j' to '\u0323')
    fun render(raw: String): String {
        var text=""; var tone:Char?=null
        raw.lowercase().forEach { c ->
            if(c in tones && text.any { it in "aăâeêioôơuưy" }) { tone=tones[c];return@forEach }
            if(c=='z') { tone=null; return@forEach }
            val tail=text.lastOrNull()
            val replacement=when("$tail$c") {
                "aa" -> "â";"aw" -> "ă";"ee" -> "ê";"oo" -> "ô";"ow" -> "ơ";"uw" -> "ư";"dd" -> "đ";else -> null
            }
            if(c=='w' && text.endsWith("uo"))text=text.dropLast(2)+"ươ"
            else if(replacement!=null)text=text.dropLast(1)+replacement
            else text+=c
        }
        fun capital(value: String) = if(raw.filter { it.isLetter() }.all { it.isUpperCase() }) value.uppercase() else if(raw.firstOrNull()?.isUpperCase()==true) value.replaceFirstChar { it.uppercaseChar() } else value
        if(tone==null)return capital(text)
        var indices=text.indices.filter { text[it] in "aăâeêioôơuưy" }
        if(indices.size>1 && text.startsWith("qu"))indices=indices.filter { it!=1 }
        if(indices.size>1 && text.startsWith("gi"))indices=indices.filter { it!=1 }
        if(indices.isEmpty())return text
        val preferred=indices.lastOrNull { text[it] in "êơ" }
        val target=preferred ?: when {
            indices.size>=3 -> indices[indices.size-2]
            indices.size==2 && indices.last()!=text.lastIndex -> indices.last()
            else -> indices.first()
        }
        return capital(Normalizer.normalize(text.substring(0,target+1)+tone+text.substring(target+1),Normalizer.Form.NFC))
    }
}
