package com.example.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class StreakStats(
    val rosarioTotal: Int = 0,
    val coronillaTotal: Int = 0,
    val rachaRosarioDias: Int = 0,
    val rachaCoronillaDias: Int = 0,
    val rosarioUltima: String? = null,
    val coronillaUltima: String? = null
)

/**
 * Calculates current prayer streak days based on the last recorded date and count.
 * Rules:
 * - If last date is null or blank -> 0
 * - If more than 1 day has passed between last date and today -> 0
 * - If last date is in the future (diff < 0) -> 0
 * - Otherwise (diff == 0 [today] or diff == 1 [yesterday]) -> count (defaults to 1 if count is null/blank)
 */
fun calcularRacha(
    ultimaFechaIso: String?,
    countRaw: String?,
    hoy: LocalDate
): Int {
    if (ultimaFechaIso.isNullOrBlank()) return 0

    val ultimaFecha = try {
        LocalDate.parse(ultimaFechaIso.trim())
    } catch (_: Exception) {
        return 0
    }

    val diff = ChronoUnit.DAYS.between(ultimaFecha, hoy)
    if (diff > 1 || diff < 0) {
        return 0
    }

    val count = countRaw?.trim()?.toIntOrNull() ?: 1
    return count
}
