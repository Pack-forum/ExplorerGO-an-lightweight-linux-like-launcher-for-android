package com.explorer.launcher

import java.util.Locale

/** Dil sistemi: kaynak metinler Türkçe; seçilen dile göre sözlükten çevrilir. */
object L {
    private val sys: Locale = Locale.getDefault()
    var lang = "tr"
    fun refresh() {
        lang = when (P.i("lang", 0)) { 1 -> "tr"; 2 -> "en"; else -> if (sys.language == "tr") "tr" else "en" }
        Locale.setDefault(Locale(lang))
    }
    fun t(s: String): String = if (lang == "en") (EN[s] ?: s) else s
    val EN: Map<String, String> = EN_MAP
}
