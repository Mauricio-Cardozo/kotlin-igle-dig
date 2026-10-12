package com.example.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

val DIAS_SEMANA = listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
val MESES_ANIO = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

/**
 * Provider interface to allow mocking or fixing date/time in tests.
 */
interface DateProvider {
    fun today(): LocalDate
    fun nowTime(): LocalTime
}

class DefaultDateProvider(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : DateProvider {
    override fun today(): LocalDate = LocalDate.now(zoneId)
    override fun nowTime(): LocalTime = LocalTime.now(zoneId)
}

class FixedDateProvider(
    private val fixedDate: LocalDate,
    private val fixedTime: LocalTime = LocalTime.of(10, 0)
) : DateProvider {
    override fun today(): LocalDate = fixedDate
    override fun nowTime(): LocalTime = fixedTime
}

/**
 * Returns today's date formatted as YYYY-MM-DD.
 */
fun hoy(date: LocalDate = LocalDate.now()): String {
    val month = date.monthValue.toString().padStart(2, '0')
    val day = date.dayOfMonth.toString().padStart(2, '0')
    return "${date.year}-$month-$day"
}

/**
 * Formats an ISO string YYYY-MM-DD into Spanish readable date.
 * E.g. "2026-07-09" -> "Jueves, 9 de Julio 2026"
 */
fun formatoFecha(iso: String): String {
    val parts = iso.split("-").map { it.toInt() }
    val y = parts[0]
    val m = parts[1]
    val d = parts[2]
    val date = LocalDate.of(y, m, d)
    val dayIndex = date.dayOfWeek.value % 7 // 0=Domingo, 1=Lunes, ..., 6=Sábado
    val dia = DIAS_SEMANA[dayIndex]
    val mes = MESES_ANIO[m - 1]
    return "$dia, $d de $mes $y"
}

/**
 * Formats the given date into the long header format used in the app.
 * E.g. "Domingo, 11 de Octubre"
 */
fun fechaActualLarga(date: LocalDate = LocalDate.now()): String {
    val dayIndex = date.dayOfWeek.value % 7 // 0=Domingo, 1=Lunes, ..., 6=Sábado
    val dia = DIAS_SEMANA[dayIndex]
    val mes = MESES_ANIO[date.monthValue - 1]
    return "$dia, ${date.dayOfMonth} de $mes"
}

/**
 * Greeting based on the hour of the day.
 * < 6 -> "Bendecida noche"
 * < 12 -> "Bendecido día"
 * < 20 -> "Bendecida tarde"
 * else -> "Bendecida noche"
 */
fun saludo(hour: Int): String {
    return when {
        hour < 6 -> "Bendecida noche"
        hour < 12 -> "Bendecido día"
        hour < 20 -> "Bendecida tarde"
        else -> "Bendecida noche"
    }
}
