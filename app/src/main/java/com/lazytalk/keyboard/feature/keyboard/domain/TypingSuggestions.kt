package com.lazytalk.keyboard.feature.keyboard.domain

interface TypingSuggestions {
    fun candidates(engine: String, input: String): List<String>
    object None : TypingSuggestions { override fun candidates(engine: String, input: String) = emptyList<String>() }
}

/** Roman letters are kept separately so deletion can undo one physical key. */
object KanaInput {
    private val table = buildMap<String, String> {
        listOf("a" to "あいうえお", "k" to "かきくけこ", "s" to "さしすせそ", "t" to "たちつてと",
            "n" to "なにぬねの", "h" to "はひふへほ", "m" to "まみむめも", "r" to "らりるれろ",
            "g" to "がぎぐげご", "z" to "ざじずぜぞ", "d" to "だぢづでど", "b" to "ばびぶべぼ", "p" to "ぱぴぷぺぽ").forEach { (c, row) ->
            "aiueo".forEachIndexed { i,v -> put((if(c=="a") "" else c)+v, row[i].toString()) }
        }
        putAll(mapOf("shi" to "し", "chi" to "ち", "tsu" to "つ", "fu" to "ふ", "ji" to "じ",
            "ya" to "や", "yu" to "ゆ", "yo" to "よ", "wa" to "わ", "wo" to "を", "nn" to "ん", "n'" to "ん"))
        mapOf("ky" to "き", "gy" to "ぎ", "sh" to "し", "sy" to "し", "ch" to "ち", "ty" to "ち", "ny" to "に",
            "hy" to "ひ", "by" to "び", "py" to "ぴ", "my" to "み", "ry" to "り", "j" to "じ", "zy" to "じ").forEach { (c,k) ->
            listOf("a" to "ゃ", "u" to "ゅ", "o" to "ょ").forEach { (v,y) -> put(c+v,k+y) }
        }
        listOf("x", "l").forEach { c ->
            "aiueo".forEachIndexed { i,v -> put(c+v,"ぁぃぅぇぉ"[i].toString()) }
            put(c+"tsu","っ"); put(c+"tu","っ"); put(c+"ya","ゃ"); put(c+"yu","ゅ"); put(c+"yo","ょ")
        }
        putAll(mapOf("fa" to "ふぁ", "fi" to "ふぃ", "fe" to "ふぇ", "fo" to "ふぉ", "va" to "ゔぁ", "vi" to "ゔぃ", "vu" to "ゔ", "ve" to "ゔぇ", "vo" to "ゔぉ", "-" to "ー"))
    }
    fun render(raw: String, finish: Boolean = false): String = buildString {
        val input=raw.lowercase(); var i=0
        while(i<input.length) {
            if(input[i]=='n' && i+2<input.length && input[i+1]=='n' && input[i+2] in "aiueoy") { append("ん");i++;continue }
            if(i+1<input.length && input[i]==input[i+1] && input[i] in "bcdfghjklmpqrstvwxyz") { append("っ"); i++; continue }
            if(input[i]=='n' && i+1<input.length && input[i+1] !in "aiueoyn'") { append("ん"); i++; continue }
            val match=(minOf(4,input.length-i) downTo 1).map { input.substring(i,i+it) }.firstOrNull { it in table }
            if(match!=null) { append(table.getValue(match));i+=match.length }
            else { append(if(finish && i==input.lastIndex && input[i]=='n') "ん" else input[i]);i++ }
        }
    }
    fun katakana(kana: String) = kana.map { if(it in 'ぁ'..'ゖ') (it.code+0x60).toChar() else it }.joinToString("")
}

object HangulInput {
    private const val leading="ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
    private const val vowels="ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"
    private const val trailing=" ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"
    private val vowelPairs=mapOf("ㅗㅏ" to 'ㅘ',"ㅗㅐ" to 'ㅙ',"ㅗㅣ" to 'ㅚ',"ㅜㅓ" to 'ㅝ',"ㅜㅔ" to 'ㅞ',"ㅜㅣ" to 'ㅟ',"ㅡㅣ" to 'ㅢ')
    private val tailPairs=mapOf("ㄱㅅ" to 'ㄳ',"ㄴㅈ" to 'ㄵ',"ㄴㅎ" to 'ㄶ',"ㄹㄱ" to 'ㄺ',"ㄹㅁ" to 'ㄻ',"ㄹㅂ" to 'ㄼ',"ㄹㅅ" to 'ㄽ',"ㄹㅌ" to 'ㄾ',"ㄹㅍ" to 'ㄿ',"ㄹㅎ" to 'ㅀ',"ㅂㅅ" to 'ㅄ')
    fun render(raw: String): String = buildString {
        var l:Char?=null;var v:Char?=null;var t:Char?=null
        fun emit() { if(l!=null && v!=null) append((0xAC00+(leading.indexOf(l!!)*21+vowels.indexOf(v!!))*28+(t?.let { trailing.indexOf(it) }?:0)).toChar()) else { l?.let { append(it) };v?.let { append(it) } };l=null;v=null;t=null }
        raw.forEach { c ->
            if(c in vowels) {
                if(v==null) v=c
                else if(t!=null) {
                    val pair=tailPairs.entries.firstOrNull { it.value==t }
                    val next=if(pair!=null) pair.key[1] else t!!
                    t=pair?.key?.get(0);emit();l=next;v=c
                } else { val combined=vowelPairs["$v$c"];if(combined!=null)v=combined else {emit();v=c} }
            } else if(c in leading) {
                if(l==null && v==null) l=c
                else if(l!=null && v!=null && c in trailing) {
                    if(t==null)t=c else { val combined=tailPairs["$t$c"];if(combined!=null)t=combined else {emit();l=c} }
                } else {emit();l=c}
            } else {emit();append(c)}
        };emit()
    }
}
