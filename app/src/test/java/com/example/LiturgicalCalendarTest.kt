package com.example

import com.example.util.domingoDePascua
import com.example.util.esAdviento
import com.example.util.esCuaresma
import com.example.util.miercolesDeCeniza
import com.example.util.primerDomingoDeAdviento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class LiturgicalCalendarTest {

    @Test
    fun domingoDePascua_matchesTheKnownGregorianDates() {
        assertEquals(LocalDate.of(2024, 3, 31), domingoDePascua(2024))
        assertEquals(LocalDate.of(2025, 4, 20), domingoDePascua(2025))
        assertEquals(LocalDate.of(2026, 4, 5), domingoDePascua(2026))
        assertEquals(LocalDate.of(2027, 3, 28), domingoDePascua(2027))
        assertEquals(LocalDate.of(2030, 4, 21), domingoDePascua(2030))
    }

    @Test
    fun miercolesDeCeniza_isFortySixDaysBeforeEaster() {
        assertEquals(LocalDate.of(2026, 2, 18), miercolesDeCeniza(2026))
        assertEquals(LocalDate.of(2027, 2, 10), miercolesDeCeniza(2027))
    }

    @Test
    fun primerDomingoDeAdviento_isTheFourthSundayBeforeChristmas() {
        assertEquals(LocalDate.of(2025, 11, 30), primerDomingoDeAdviento(2025))
        assertEquals(LocalDate.of(2026, 11, 29), primerDomingoDeAdviento(2026))
        assertEquals(LocalDate.of(2027, 11, 28), primerDomingoDeAdviento(2027))
        assertEquals(DayOfWeek.SUNDAY, primerDomingoDeAdviento(2026).dayOfWeek)
    }

    @Test
    fun esAdviento_coversFromTheFirstSundayUntilChristmasEve() {
        assertFalse(esAdviento(LocalDate.of(2026, 11, 28))) // sábado previo
        assertTrue(esAdviento(LocalDate.of(2026, 11, 29)))
        assertTrue(esAdviento(LocalDate.of(2026, 12, 24)))
        assertFalse(esAdviento(LocalDate.of(2026, 12, 25))) // Navidad
    }

    @Test
    fun esCuaresma_coversFromAshWednesdayUntilHolySaturday() {
        assertFalse(esCuaresma(LocalDate.of(2026, 2, 17))) // martes anterior
        assertTrue(esCuaresma(LocalDate.of(2026, 2, 18)))
        assertTrue(esCuaresma(LocalDate.of(2026, 2, 22))) // primer domingo de Cuaresma
        assertTrue(esCuaresma(LocalDate.of(2026, 4, 4))) // Sábado Santo
        assertFalse(esCuaresma(LocalDate.of(2026, 4, 5))) // Pascua
    }
}
