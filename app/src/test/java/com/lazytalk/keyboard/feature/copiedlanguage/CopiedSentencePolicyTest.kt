package com.lazytalk.keyboard.feature.copiedlanguage

import org.junit.Assert.*
import org.junit.Test

class CopiedSentencePolicyTest {
    @Test fun separatesSentencesAndLinesForExplicitSelection() {
        assertEquals(listOf("Where are you now?", "I will arrive tomorrow.", "Awak dekat mana sekarang?"),
            CopiedSentencePolicy.sentences("Where are you now? I will arrive tomorrow.\nAwak dekat mana sekarang?"))
        assertEquals(listOf("你现在在哪里？", "我明天过去。"), CopiedSentencePolicy.sentences("你现在在哪里？我明天过去。"))
    }
    @Test fun rejectsEmptyOrOversizedClips() {
        assertTrue(CopiedSentencePolicy.sentences("  \n ").isEmpty())
        assertTrue(CopiedSentencePolicy.sentences("x".repeat(4001)).isEmpty())
    }
    @Test fun uncertaintyAndUnsupportedLanguagesCannotReplaceTarget() {
        val supported = setOf("en", "ms", "id", "zh")
        listOf(listOf("und" to 1f), listOf("ms" to .6f), listOf("ms" to .85f, "id" to .7f),
            listOf("zz" to .99f)).forEach { assertNull(CopiedSentencePolicy.language(it, supported)) }
        assertEquals("ms", CopiedSentencePolicy.language(listOf("ms" to .99f), supported))
        assertEquals("zh", CopiedSentencePolicy.language(listOf("zh-Hans" to .99f), supported))
    }
}
