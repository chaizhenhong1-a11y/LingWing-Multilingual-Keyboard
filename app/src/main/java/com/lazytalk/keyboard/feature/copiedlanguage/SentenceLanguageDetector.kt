package com.lazytalk.keyboard.feature.copiedlanguage

import java.text.BreakIterator
import java.util.Locale

interface SentenceLanguageDetector {
    suspend fun identify(text: String): List<Pair<String, Float>>
    fun close() {}
}

object CopiedSentencePolicy {
    fun sentences(text: String): List<String> {
        if (text.length > 4000) return emptyList()
        return text.lineSequence().flatMap { line ->
            val iterator = BreakIterator.getSentenceInstance(Locale.ROOT)
            iterator.setText(line)
            val parts = mutableListOf<String>()
            var start = iterator.first()
            var end = iterator.next()
            while (end != BreakIterator.DONE) {
                line.substring(start, end).trim().takeIf(String::isNotBlank)?.let(parts::add)
                start = end; end = iterator.next()
            }
            parts.asSequence()
        }.take(50).toList()
    }
    fun language(scores: List<Pair<String, Float>>, supported: Set<String>): String? {
        val ranked = scores.filter { it.first != "und" }.sortedByDescending { it.second }
        val best = ranked.firstOrNull() ?: return null
        if (best.second < .80f || best.second - (ranked.getOrNull(1)?.second ?: 0f) < .20f) return null
        val code = when (val base = best.first.substringBefore('-')) { "tl" -> "fil"; "iw" -> "he"; else -> base }
        return code.takeIf { it in supported }
    }
}
