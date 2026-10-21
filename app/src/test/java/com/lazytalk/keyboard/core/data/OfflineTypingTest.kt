package com.lazytalk.keyboard.core.data

import android.app.Application
import com.lazytalk.keyboard.core.model.TypingLayouts
import com.lazytalk.keyboard.feature.keyboard.domain.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class OfflineTypingTest {
    private fun repo(): OfflineTypingSuggestions {
        val context=RuntimeEnvironment.getApplication()
        return OfflineTypingSuggestions { context.assets.open(it).bufferedReader().use { r->r.readText() } }
    }
    @Test fun realBundledPinyinDictionaryIncludesWordsAndSegmentsLongerInput() {
        val r=repo()
        assertTrue("你好" in r.candidates("pinyin","nihao"))
        assertTrue("中国" in r.candidates("pinyin","zhongguo"))
        assertTrue(r.candidates("pinyin","woaini").any { it.contains("我") && it.contains("爱") })
    }
    @Test fun initialsAndMixedPinyinUseRealBundledWords() {
        val r=repo()
        assertTrue("你好" in r.candidates("pinyin","nh"))
        assertTrue("你好了吗" in r.candidates("pinyin","nhlm"))
        assertTrue("你好了吗" in r.candidates("pinyin","nihaolm"))
        assertTrue("中国" in r.candidates("pinyin","zg"))
        assertTrue("你好了吗" in r.candidates("pinyin","NHLM"))
        assertEquals("你好",r.candidates("pinyin","nihao").first())
    }
    @Test fun initialsAreDerivedForWordsOutsideTheBundledExamplesAndKeepAmbiguousReadings() {
        val r=OfflineTypingSuggestions { "bei\t北\t1000\njing\t京\t900\nbeijing\t北京\t800\nxi\t西\t700\nan\t安\t600\nxian\t先\t500\nxian\t西安\t400" }
        assertTrue("北京" in r.candidates("pinyin","bj"))
        assertTrue("西安" in r.candidates("pinyin","xa"))
        assertTrue("先" in r.candidates("pinyin","x"))
    }
    @Test fun japaneseRomajiAndActualWordDictionary() {
        assertEquals("こんにちは",KanaInput.render("konnichiha",true))
        assertEquals("がっこう",KanaInput.render("gakkou",true))
        val result=repo().candidates("kana","nihon")
        assertTrue("日本" in result)
        assertTrue("にほん" in result)
        assertTrue("ニホン" in result)
    }
    @Test fun hangulCombinesSyllablesResyllabifiesAndDeletesByReplayingKeys() {
        assertEquals("한글",HangulInput.render("ㅎㅏㄴㄱㅡㄹ"))
        assertEquals("가나",HangulInput.render("ㄱㅏㄴㅏ"))
        assertEquals("과",HangulInput.render("ㄱㅗㅏ"))
        assertEquals("읽어",HangulInput.render("ㅇㅣㄹㄱㅇㅓ"))
        assertEquals("한그",HangulInput.render("ㅎㅏㄴㄱㅡ"))
    }
    @Test fun telexHandlesShapeAndToneMarks() {
        assertEquals("tiếng",TelexInput.render("tieengs"))
        assertEquals("việt",TelexInput.render("vieetj"))
        assertEquals("Việt",TelexInput.render("Vieetj"))
        assertEquals("VIỆT",TelexInput.render("VIEETJ"))
        assertEquals("nước",TelexInput.render("nuowcs"))
        assertEquals("đường",TelexInput.render("dduowngf"))
    }
    @Test fun importedLayoutsPreserveMulticodepointKeysAndLanguageAccents() {
        val app=RuntimeEnvironment.getApplication()
        val layouts=TypingLayoutLoader.load(app.assets)
        assertTrue(layouts.size>=60)
        assertTrue(layouts.any { it.tag.startsWith("hi") && it.tokenRows.flatten().any { s->s.length>1 } })
        assertTrue(layouts.any { it.tag.startsWith("fr") && it.popups.values.flatten().any { s->s=="é" } })
        assertTrue(layouts.any { it.tag.startsWith("de") && "ü" in it.tokenRows.flatten() })
        assertEquals(layouts.size,layouts.map { it.id }.distinct().size)
        assertTrue(TypingLayouts.all.any { it.engine=="pinyin" })
    }
}
