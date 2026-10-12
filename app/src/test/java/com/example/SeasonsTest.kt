package com.example

import com.example.util.detectSeason
import com.example.util.isSunday
import com.example.util.parseWeekNumber
import com.example.util.romanToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonsTest {

    @Test
    fun romanToInt_convertsRomanNumerals() {
        assertEquals(1, romanToInt("I"))
        assertEquals(4, romanToInt("IV"))
        assertEquals(10, romanToInt("X"))
        assertEquals(34, romanToInt("XXXIV"))
    }

    @Test
    fun romanToInt_returnsZeroForUnknown() {
        assertEquals(0, romanToInt(""))
        assertEquals(0, romanToInt("FOO"))
    }

    @Test
    fun detectSeason_detectsAdviento() {
        assertEquals("adviento", detectSeason("Domingo I de Adviento"))
    }

    @Test
    fun detectSeason_detectsNavidad() {
        assertEquals("navidad", detectSeason("Natividad del Señor (Navidad)"))
    }

    @Test
    fun detectSeason_detectsCuaresma() {
        assertEquals("cuaresma", detectSeason("Domingo I de Cuaresma"))
    }

    @Test
    fun detectSeason_detectsPascua() {
        assertEquals("pascua", detectSeason("Domingo de Pascua"))
    }

    @Test
    fun detectSeason_detectsOrdinario() {
        assertEquals("ordinario", detectSeason("Domingo XII del Tiempo Ordinario"))
    }

    @Test
    fun detectSeason_returnsNullForUnknown() {
        assertNull(detectSeason("Fiesta de la Sagrada Familia"))
    }

    @Test
    fun parseWeekNumber_extractsRomanNumeral() {
        assertEquals(12, parseWeekNumber("Domingo XII del Tiempo Ordinario"))
        assertEquals(3, parseWeekNumber("Domingo III de Adviento"))
    }

    @Test
    fun parseWeekNumber_returnsZeroIfNoRomanNumeral() {
        assertEquals(0, parseWeekNumber("Natividad del Señor"))
    }

    @Test
    fun isSunday_detectsDomingo() {
        assertTrue(isSunday("Domingo XII del Tiempo Ordinario"))
    }

    @Test
    fun isSunday_returnsFalseForWeekdays() {
        assertFalse(isSunday("Feria IV de Tiempo Ordinario"))
    }
}
