package com.example

import com.example.data.repository.BibliaRepository
import com.example.data.user.UserPreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class LogicUnitTest {

    @Test
    fun buildFtsQuery_singleWord_quotedCorrectly() {
        val query = "amor"
        val expected = "\"amor\""
        val actual = BibliaRepository.buildFtsQuery(query)
        assertEquals(expected, actual)
    }

    @Test
    fun buildFtsQuery_multipleWords_separatedAndQuoted() {
        val query = "amor prójimo"
        val expected = "\"amor\" \"prójimo\""
        val actual = BibliaRepository.buildFtsQuery(query)
        assertEquals(expected, actual)
    }

    @Test
    fun buildFtsQuery_extraSpacesAndQuotes_cleanedAndJoined() {
        val query = "   \"amor\"    prójimo   paz  "
        val expected = "\"amor\" \"prójimo\" \"paz\""
        val actual = BibliaRepository.buildFtsQuery(query)
        assertEquals(expected, actual)
    }

    @Test
    fun buildFtsQuery_emptyOrBlank_returnsEmpty() {
        assertEquals("", BibliaRepository.buildFtsQuery(""))
        assertEquals("", BibliaRepository.buildFtsQuery("   "))
    }

    @Test
    fun fontSizeMultiplier_defaultIsOne() {
        assertEquals(1.0f, UserPreferencesRepository.DEFAULT_FONT_SCALE, 0.001f)
    }

    @Test
    fun fontSizeMultiplier_increment_stepsByPointOne() {
        val current = 1.0f
        val incremented = UserPreferencesRepository.increment(current)
        assertEquals(1.1f, incremented, 0.001f)
    }

    @Test
    fun fontSizeMultiplier_decrement_stepsByPointOne() {
        val current = 1.0f
        val decremented = UserPreferencesRepository.decrement(current)
        assertEquals(0.9f, decremented, 0.001f)
    }

    @Test
    fun fontSizeMultiplier_clampedAtMinAndMax() {
        val belowMin = UserPreferencesRepository.sanitizeMultiplier(0.5f)
        assertEquals(0.8f, belowMin, 0.001f)

        val aboveMax = UserPreferencesRepository.sanitizeMultiplier(2.0f)
        assertEquals(1.5f, aboveMax, 0.001f)
    }

    @Test
    fun fontSizeMultiplier_roundingAvoidsFloatingPointDrift() {
        // Floating point 0.8 + 0.1 might yield 0.90000004
        val step = UserPreferencesRepository.increment(0.8f)
        assertEquals(0.9f, step, 0.001f)
    }
}
