package com.example

import com.example.util.calcularNuevaRacha
import com.example.util.calcularRacha
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreaksTest {

    private val hoy = LocalDate.of(2026, 10, 12)

    @Test
    fun calcularRacha_returnsZeroWhenNoStoredDate() {
        assertEquals(0, calcularRacha(null, "5", hoy))
        assertEquals(0, calcularRacha("", "5", hoy))
        assertEquals(0, calcularRacha("   ", "5", hoy))
    }

    @Test
    fun calcularRacha_returnsCountWhenDoneToday() {
        val todayIso = "2026-10-12"
        assertEquals(5, calcularRacha(todayIso, "5", hoy))
        assertEquals(1, calcularRacha(todayIso, null, hoy)) // Default count 1
        assertEquals(1, calcularRacha(todayIso, "invalid", hoy))
    }

    @Test
    fun calcularRacha_returnsCountWhenDoneYesterday() {
        val ayerIso = "2026-10-11"
        assertEquals(7, calcularRacha(ayerIso, "7", hoy))
        assertEquals(1, calcularRacha(ayerIso, null, hoy))
    }

    @Test
    fun calcularRacha_returnsZeroWhenDoneBeforeYesterday() {
        val anteayerIso = "2026-10-10" // 2 days ago
        assertEquals(0, calcularRacha(anteayerIso, "10", hoy))

        val lastWeekIso = "2026-10-05"
        assertEquals(0, calcularRacha(lastWeekIso, "20", hoy))
    }

    @Test
    fun calcularRacha_returnsZeroForFutureDatesOrInvalid() {
        val futureIso = "2026-10-13"
        assertEquals(0, calcularRacha(futureIso, "5", hoy))

        val invalidIso = "not-a-date"
        assertEquals(0, calcularRacha(invalidIso, "5", hoy))
    }

    @Test
    fun calcularNuevaRacha_startsAtOneWithoutPreviousDate() {
        assertEquals(1, calcularNuevaRacha(null, null, hoy))
        assertEquals(1, calcularNuevaRacha("", "9", hoy))
        assertEquals(1, calcularNuevaRacha("   ", "9", hoy))
        assertEquals(1, calcularNuevaRacha("not-a-date", "9", hoy))
    }

    @Test
    fun calcularNuevaRacha_keepsCountWhenAlreadyPrayedToday() {
        assertEquals(5, calcularNuevaRacha("2026-10-12", "5", hoy))
        // Sin conteo previo válido, la racha de hoy vale 1.
        assertEquals(1, calcularNuevaRacha("2026-10-12", null, hoy))
        assertEquals(1, calcularNuevaRacha("2026-10-12", "invalid", hoy))
        assertEquals(1, calcularNuevaRacha("2026-10-12", "0", hoy))
    }

    @Test
    fun calcularNuevaRacha_incrementsWhenPrayedYesterday() {
        assertEquals(8, calcularNuevaRacha("2026-10-11", "7", hoy))
        assertEquals(2, calcularNuevaRacha("2026-10-11", null, hoy))
    }

    @Test
    fun calcularNuevaRacha_resetsWhenGapIsLongerOrTheDateIsInTheFuture() {
        assertEquals(1, calcularNuevaRacha("2026-10-10", "20", hoy))
        assertEquals(1, calcularNuevaRacha("2026-10-05", "20", hoy))
        // Reloj movido hacia atrás: la fecha guardada queda en el futuro y la racha arranca de nuevo.
        assertEquals(1, calcularNuevaRacha("2026-10-13", "20", hoy))
    }
}
