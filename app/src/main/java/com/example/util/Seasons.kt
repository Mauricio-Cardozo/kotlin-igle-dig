package com.example.util

val SEASON_EMOJI = mapOf(
    "adviento" to "🕯️",
    "navidad" to "⭐",
    "cuaresma" to "🙏",
    "pascua" to "✨"
)

val SEASON_COLOR = mapOf(
    "adviento" to "#7B3FAF",
    "cuaresma" to "#7B3FAF",
    "pascua" to "#E8C97A",
    "navidad" to "#E8C97A",
    "ordinario" to "#4CAF50"
)

val SEASON_COLOR_NAME = mapOf(
    "adviento" to "Morado",
    "cuaresma" to "Morado",
    "pascua" to "Blanco",
    "navidad" to "Blanco",
    "ordinario" to "Verde"
)

private val ROMAN_MAP = mapOf(
    "I" to 1, "II" to 2, "III" to 3, "IV" to 4, "V" to 5, "VI" to 6, "VII" to 7, "VIII" to 8, "IX" to 9,
    "X" to 10, "XI" to 11, "XII" to 12, "XIII" to 13, "XIV" to 14, "XV" to 15, "XVI" to 16,
    "XVII" to 17, "XVIII" to 18, "XIX" to 19, "XX" to 20, "XXI" to 21, "XXII" to 22, "XXIII" to 23,
    "XXIV" to 24, "XXV" to 25, "XXVI" to 26, "XXVII" to 27, "XXVIII" to 28, "XXIX" to 29,
    "XXX" to 30, "XXXI" to 31, "XXXII" to 32, "XXXIII" to 33, "XXXIV" to 34
)

fun romanToInt(s: String): Int {
    return ROMAN_MAP[s.uppercase()] ?: 0
}

fun detectSeason(tituloMisa: String): String? {
    val t = tituloMisa.lowercase()
    return when {
        t.contains("adviento") -> "adviento"
        t.contains("navidad") -> "navidad"
        t.contains("cuaresma") -> "cuaresma"
        t.contains("pascua") -> "pascua"
        t.contains("ordinario") || t.contains("tiempo ordinario") -> "ordinario"
        else -> null
    }
}

fun parseWeekNumber(tituloMisa: String): Int {
    val romanPattern = Regex("""\b([IVXLCDM]{1,6})\b""")
    val match = romanPattern.find(tituloMisa.uppercase())
    return match?.groupValues?.get(1)?.let { romanToInt(it) } ?: 0
}

fun isSunday(tituloMisa: String): Boolean {
    return Regex("domingo", RegexOption.IGNORE_CASE).containsMatchIn(tituloMisa)
}
