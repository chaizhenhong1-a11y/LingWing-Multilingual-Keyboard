package com.lazytalk.keyboard.core.model

/** Bundled direct-input layouts. Translation languages are a separate catalog. */
data class TypingLayout(val id: String, val name: String, val rows: List<String>,
    val shifted: List<String> = rows.map { it.uppercase(java.util.Locale.ROOT) },
    val stagger: Boolean = true,
    val tokenRows: List<List<String>> = rows.map { row -> row.map { it.toString() } },
    val shiftedTokenRows: List<List<String>> = shifted.map { row -> row.map { it.toString() } },
    val popups: Map<String, List<String>> = emptyMap(), val variant: String = "",
    val tag: String = id, val engine: String = "direct") {
    val chineseName: String get() = when (id) {
        "zh" -> "中文"; "ja" -> "日语"; "ko" -> "韩语"; "vi" -> "越南语"; "en" -> "英语"; "th" -> "泰语"; "ru" -> "俄语"; "uk" -> "乌克兰语"
        "ar" -> "阿拉伯语"; "el" -> "希腊语"; "he" -> "希伯来语"
        else -> if (id == "iban") "伊班语" else java.util.Locale.forLanguageTag(tag).getDisplayLanguage(java.util.Locale.SIMPLIFIED_CHINESE).ifBlank { name }
    }
    val label: String get() = "$chineseName · $name"
}

object TypingLayouts {
    private val directBuiltIn = listOf(
        TypingLayout("en", "English", listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")),
        TypingLayout("th", "ไทย", listOf("ๅ/-ภถุึคตจขช", "ๆไำพะัีรนยบล", "ฟหกดเ้่าสวงฃ", "ผปแอิืทมใฝ"),
            listOf("+๑๒๓๔ู฿๕๖๗๘๙", "๐\"ฎฑธํ๊ณฯญฐ,", "ฤฆฏโฌ็๋ษศซ.ฅ", "()ฉฮฺ์?ฒฬฦ"), false),
        TypingLayout("ru", "Русский", listOf("йцукенгшщзхъ", "фывапролджэ", "ячсмитьбюё"), stagger=false),
        TypingLayout("uk", "Українська", listOf("йцукенгшщзхї", "фівапролджє", "ячсмитьбюґ"), stagger=false),
        TypingLayout("ar", "العربية", listOf("ضصثقفغعهخحج", "شسيبلاتنمكط", "ذئءؤرىةوزظد"),
            listOf("ًٌَُإأآٍِّْ", "شسيبلاتنمكط", "ذئءؤرىةوزظد"), false),
        TypingLayout("el", "Ελληνικά", listOf(";ςερτυθιοπ", "ασδφγηξκλ", "ζχψωβνμ", "άέήίόύώϊϋΐΰ"), stagger=false),
        TypingLayout("he", "עברית", listOf("קראטוןםפ", "שדגכעיחלךף", "זסבהנמצתץ"), stagger=false)
    )
    private val latin = directBuiltIn.first()
    private val builtIn = listOf(latin,
        TypingLayout("zh", "中文（简体）", latin.rows, variant = "全拼 · 候选选字", engine = "pinyin"),
        TypingLayout("ja", "日本語", latin.rows, variant = "罗马字 → 假名 / 汉字词候选", engine = "kana"),
        TypingLayout("ko", "한국어", listOf("ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ", "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ", "ㅋㅌㅊㅍㅠㅜㅡ"),
            listOf("ㅃㅉㄸㄲㅆㅛㅕㅑㅒㅖ", "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ", "ㅋㅌㅊㅍㅠㅜㅡ"), variant = "韩文两套式 · 音节组合", engine = "hangul"),
        TypingLayout("vi", "Tiếng Việt", latin.rows, variant = "Telex · 越南语声调", engine = "telex")
    ) + directBuiltIn.drop(1)
    @Volatile private var extras: List<TypingLayout> = emptyList()
    val all: List<TypingLayout> get() = builtIn + extras
    @Synchronized fun install(layouts: List<TypingLayout>) {
        require(layouts.map { it.id }.distinct().size == layouts.size)
        require(layouts.all { it.tokenRows.isNotEmpty() && it.tokenRows.all { row -> row.isNotEmpty() && row.size <= 16 } })
        extras = layouts.filter { layout -> builtIn.none { it.id == layout.id } }
    }
    fun find(id: String) = all.firstOrNull { it.id == id } ?: all.first()
}
