package com.example.util

import com.example.data.db.Santo

fun normalizeKey(word: String): String {
    return word
        .replace("[áä]".toRegex(), "a")
        .replace("[éë]".toRegex(), "e")
        .replace("[íï]".toRegex(), "i")
        .replace("[óö]".toRegex(), "o")
        .replace("[úü]".toRegex(), "u")
        .replace("[ñ]".toRegex(), "n")
        .replace("[^a-z]".toRegex(), "")
        .replace("([a-z])\\1+".toRegex(), "$1")
}

fun santoKey(nombre: String): String {
    val tokens = nombre.lowercase().split(Regex("\\s+"))
    val secondOrFirst = tokens.getOrNull(1) ?: nombre.lowercase()
    return normalizeKey(secondOrFirst)
}

fun levenshtein(a: String, b: String): Int {
    val m = a.length
    val n = b.length
    val dp = IntArray(n + 1) { it }
    for (i in 1..m) {
        var prev = dp[0]
        dp[0] = i
        for (j in 1..n) {
            val tmp = dp[j]
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            dp[j] = minOf(prev + cost, dp[j] + 1, dp[j - 1] + 1)
            prev = tmp
        }
    }
    return dp[n]
}

fun sameSaint(a: Santo, b: Santo): Boolean {
    val ap = a.nombre.split(Regex("\\s+")).drop(1)
    val bp = b.nombre.split(Regex("\\s+")).drop(1)
    val punctRegex = Regex("""[,.;:()'"!?¿¡]+$""")
    val wa = ap.map { normalizeKey(it.replace(punctRegex, "")) }
    val wb = bp.map { normalizeKey(it.replace(punctRegex, "")) }

    val shorter = if (wa.size <= wb.size) wa else wb
    val longer = if (wa.size > wb.size) wa else wb
    val longerOrig = if (wa.size > wb.size) ap else bp

    for (i in longer.indices) {
        if (i >= shorter.size || shorter[i] != longer[i]) {
            val extra = (longerOrig.getOrNull(i) ?: "").replace(punctRegex, "")
            if (extra.isEmpty()) continue
            val firstChar = extra.first()
            if (!firstChar.isUpperCase() || firstChar.isLowerCase()) return true
            if (Regex("""^[IVXLCDM]+\.?$""").matches(extra)) return true
            if (Regex("""^(De|Del|Da|Von|Van|Di|Della|Degli|Delle|Mc|Mac|Los|Las|El|La|Y|E|O)$""", RegexOption.IGNORE_CASE).matches(extra)) return true
            return false
        }
    }
    return true
}

/**
 * Groups and deduplicates saints faithfully replicating getSantosDelDia from React Native.
 */
fun deduplicateSantos(rows: List<Santo>): List<Santo> {
    val groups = mutableListOf<MutableList<Santo>>()
    for (s in rows) {
        val key = santoKey(s.nombre)
        var matched = false
        for (g in groups) {
            val gkey = santoKey(g[0].nombre)
            val gkeyPrefix = if (gkey.length > 8) gkey.substring(0, 8) else gkey
            val keyPrefix = if (key.length > 8) key.substring(0, 8) else key
            if ((gkey == key || levenshtein(gkeyPrefix, keyPrefix) <= 2) && sameSaint(g[0], s)) {
                g.add(s)
                matched = true
                break
            }
        }
        if (!matched) {
            groups.add(mutableListOf(s))
        }
    }

    return groups.map { g ->
        var best = g[0]
        for (i in 1 until g.size) {
            val s = g[i]
            var updatedBio = best.biografia
            if (s.biografia.length > best.biografia.length) {
                updatedBio = s.biografia
            }

            var updatedTitulo = best.titulo
            if (!s.titulo.isNullOrBlank() && (best.titulo == null || !best.titulo!!.contains(s.titulo))) {
                updatedTitulo = if (best.titulo != null) "${best.titulo} — ${s.titulo}" else s.titulo
            }

            var updatedNombre = best.nombre
            if (!best.nombre.contains(s.nombre)) {
                updatedNombre = "${best.nombre} / ${s.nombre}"
            }

            best = best.copy(
                nombre = updatedNombre,
                titulo = updatedTitulo,
                biografia = updatedBio
            )
        }
        best
    }
}
