package com.lazytalk.keyboard.feature.keyboard.domain
import org.junit.Assert.*
import org.junit.Test
class SwapLanguagesUseCaseTest {
    @Test fun swapsExplicitOrDetectedSource() {
        val swap = SwapLanguagesUseCase()
        assertEquals(LanguagePair("ms", "en"), swap("en", "ms", null))
        assertEquals(LanguagePair("zh", "ms"), swap("auto", "zh", "ms"))
        assertNull(swap("auto", "en", null))
    }
}
