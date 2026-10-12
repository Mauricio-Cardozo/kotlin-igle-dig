package com.example

import com.example.util.fechaActualLarga
import com.example.util.formatoFecha
import com.example.util.hoy
import com.example.util.saludo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun formatoFecha_formatsIsoToReadableSpanishDate() {
        assertEquals("Jueves, 9 de Julio 2026", formatoFecha("2026-07-09"))
        assertEquals("Jueves, 1 de Enero 2026", formatoFecha("2026-01-01"))
        assertEquals("Viernes, 25 de Diciembre 2026", formatoFecha("2026-12-25"))
    }

    @Test
    fun fechaActualLarga_returnsNonEmptyString() {
        val result = fechaActualLarga()
        assertTrue(result.isNotBlank())
    }

    @Test
    fun fechaActualLarga_formatsSpecificDateCorrectly() {
        val date1 = LocalDate.of(2026, 10, 11) // Domingo
        assertEquals("Domingo, 11 de Octubre", fechaActualLarga(date1))

        val date2 = LocalDate.of(2026, 10, 12) // Lunes
        assertEquals("Lunes, 12 de Octubre", fechaActualLarga(date2))
    }

    @Test
    fun hoy_returnsYyyyMmDdFormat() {
        val result = hoy()
        assertTrue(result.matches(Regex("""^\d{4}-\d{2}-\d{2}$""")))
    }

    @Test
    fun saludo_returnsCorrectGreetingForHours() {
        assertEquals("Bendecida noche", saludo(0))
        assertEquals("Bendecida noche", saludo(5))
        assertEquals("Bendecido día", saludo(6))
        assertEquals("Bendecido día", saludo(11))
        assertEquals("Bendecida tarde", saludo(12))
        assertEquals("Bendecida tarde", saludo(19))
        assertEquals("Bendecida noche", saludo(20))
        assertEquals("Bendecida noche", saludo(23))
    }
}
