package com.lazytalk.keyboard.core.data

import com.lazytalk.keyboard.feature.keyboard.domain.*
import kotlin.math.ln

/** Bundled, deterministic word lookup and segmentation. No text leaves the device. */
class OfflineTypingSuggestions(private val read: (String) -> String) : TypingSuggestions {
    private data class Entry(val word: String, val score: Double)
    private val dictionaries = linkedMapOf<String, Map<String, List<Entry>>>()
    @Synchronized private fun dictionary(engine: String, prefix: Char): Map<String, List<Entry>> {
        val cacheKey=if(engine=="pinyin") "pinyin" else prefix.code.toString(16).padStart(4,'0')
        dictionaries[cacheKey]?.let { return it }
        val loaded = run {
        val result = linkedMapOf<String, MutableList<Entry>>()
        read(if(engine=="pinyin") "typing/pinyin.tsv" else "typing/ja/$cacheKey.tsv").lineSequence().forEach { line ->
            val f=line.split('\t'); if(f.size==3) {
                val bucket=result.getOrPut(f[0]) { mutableListOf() }
                if(bucket.size<48 && bucket.none { it.word==f[1] }) bucket+=Entry(f[1], if(engine=="pinyin") ln(f[2].toDouble()+1)-18 else -f[2].toDouble()/1000-2)
            }
        }; result
        }
        if(dictionaries.size>=4) dictionaries.remove(dictionaries.keys.first())
        dictionaries[cacheKey]=loaded
        return loaded
    }
    private var indexedPinyin: Map<String,List<Entry>>? = null
    private var initialsIndex: Map<String,List<Entry>> = emptyMap()

    /** Derive syllable initials from the bundled readings, preserving alternative syllable boundaries. */
    @Synchronized private fun initials(dict: Map<String,List<Entry>>): Map<String,List<Entry>> {
        if(indexedPinyin===dict) return initialsIndex
        val syllables=dict.filter { (reading,entries) -> reading.length<=6 && entries.any {it.word.codePointCount(0,it.word.length)==1} }.keys
        val result=hashMapOf<String,MutableList<Entry>>()
        for((reading,entries) in dict) {
            val byLength=entries.groupBy {it.word.codePointCount(0,it.word.length)}
            for((count,words) in byLength) {
                if(count !in 1..16 || reading.length<count) continue
                val memo=hashMapOf<Pair<Int,Int>,List<String>>()
                fun split(offset: Int, remaining: Int): List<String> {
                    if(remaining==0) return if(offset==reading.length) listOf("") else emptyList()
                    if(reading.length-offset<remaining || reading.length-offset>remaining*6) return emptyList()
                    return memo.getOrPut(offset to remaining) {
                        val found=mutableListOf<String>()
                        for(end in offset+1..minOf(reading.length,offset+6)) {
                            if(reading.substring(offset,end) !in syllables) continue
                            for(tail in split(end,remaining-1)) {
                                val abbreviation=reading[offset]+tail
                                if(abbreviation !in found) found+=abbreviation
                                if(found.size>=4) break
                            }
                            if(found.size>=4) break
                        }
                        found
                    }
                }
                for(abbreviation in split(0,count)) result.getOrPut(abbreviation) {mutableListOf()}.addAll(words)
            }
        }
        initialsIndex=result.mapValues { (_,entries) -> entries.sortedByDescending {it.score}.distinctBy {it.word}.take(32) }
        indexedPinyin=dict
        return initialsIndex
    }

    private data class Phrase(val text: String,val score: Double)
    private fun abbreviatedPhrases(key: String, dict: Map<String,List<Entry>>): List<String> {
        val short=initials(dict)
        val paths=Array(key.length+1) {mutableListOf<Phrase>()}
        paths[0]+=Phrase("",0.0)
        for(start in key.indices) {
            val prefixes=paths[start].sortedByDescending {it.score}.distinctBy {it.text}.take(16)
            if(prefixes.isEmpty()) continue
            for(end in start+1..minOf(key.length,start+32)) {
                val token=key.substring(start,end)
                val options=(dict[token].orEmpty()+short[token].orEmpty()).sortedByDescending {it.score}.distinctBy {it.word}.take(6)
                if(options.isEmpty()) continue
                for(prefix in prefixes) for(entry in options) paths[end]+=Phrase(prefix.text+entry.word,prefix.score+entry.score)
                // Bound the search while keeping alternate words instead of only the single best segmentation.
                if(paths[end].size>256) {
                    val best=paths[end].sortedByDescending {it.score}.distinctBy {it.text}.take(32)
                    paths[end].clear();paths[end].addAll(best)
                }
            }
        }
        return paths.last().sortedByDescending {it.score}.distinctBy {it.text}.take(32).map {it.text}
    }

    override fun candidates(engine: String, input: String): List<String> {
        if(input.isEmpty())return emptyList()
        if(input.length>96)return listOf(input)
        if(engine=="hangul")return listOf(HangulInput.render(input))
        if(engine=="telex")return listOf(TelexInput.render(input), input).distinct()
        val key=if(engine=="pinyin") input.lowercase().replace("'", "") else KanaInput.render(input,true)
        if(engine=="kana" && key.first() !in 'ぁ'..'ゖ')return listOf(key)
        val dict=dictionary(engine,key.first());val values=mutableListOf<String>()
        values+=dict[key].orEmpty().map { it.word }
        if(engine=="pinyin") {
            values+=abbreviatedPhrases(key,dict)
        }
        if(values.isEmpty() && engine=="pinyin") {
            val scores=DoubleArray(key.length+1) { Double.NEGATIVE_INFINITY }; val phrases=arrayOfNulls<String>(key.length+1)
            scores[0]=0.0;phrases[0]=""
            for(i in key.indices) if(phrases[i]!=null) {
                for(end in i+1..minOf(key.length,i+32)) {
                    val entry=dict[key.substring(i,end)]?.firstOrNull()?:continue
                    val score=scores[i]+entry.score
                    if(score>scores[end]) { scores[end]=score;phrases[end]=phrases[i]+entry.word }
                }
            }
            phrases.last()?.let { values+=it }
            if(engine=="pinyin" && values.isEmpty()) dict.asSequence().filter { it.key.startsWith(key) }.take(12).forEach { values+=it.value.take(3).map { e->e.word } }
        }
        if(engine=="kana") { values+=key;values+=KanaInput.katakana(key) }
        values+=input
        return values.distinct().take(48)
    }
}
