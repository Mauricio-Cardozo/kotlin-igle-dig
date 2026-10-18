package com.example

import com.example.ui.oracion.generarPasosRosario
import com.example.ui.oracion.misteriosDelDia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RosarioStepsTest {

    /** Lunes 12/10/2026: misterios Gozosos. */
    private val lunes = LocalDate.of(2026, 10, 12)

    /** Cuenta las apariciones no solapadas de [aguja] en el texto. */
    private fun String.veces(aguja: String): Int = split(aguja).size - 1

    @Test
    fun misteriosDelDia_matchesTheRnWeekdayAssignment() {
        val esperado = mapOf(
            DayOfWeek.MONDAY to "Gozosos",
            DayOfWeek.TUESDAY to "Dolorosos",
            DayOfWeek.WEDNESDAY to "Gloriosos",
            DayOfWeek.THURSDAY to "Luminosos",
            DayOfWeek.FRIDAY to "Dolorosos",
            DayOfWeek.SATURDAY to "Gozosos",
            DayOfWeek.SUNDAY to "Gloriosos"
        )

        esperado.forEach { (dia, nombre) ->
            val misterios = misteriosDelDia(dia)
            assertEquals("Misterios del $dia", nombre, misterios.nombre)
            assertEquals(5, misterios.misterios.size)
            assertTrue(misterios.misterios.all { it.isNotBlank() })
        }
    }

    @Test
    fun generarPasosRosario_hasTheNinetyStepsOfTheRnSequence() {
        val pasos = generarPasosRosario(lunes)

        assertEquals(90, pasos.size)
        assertEquals("senal", pasos.first().id)
        assertEquals("completado", pasos.last().id)
        assertEquals("Rosario Completado 🌹", pasos.last().title)

        // Apertura: señal, contrición, padrenuestro por el Santo Padre, tres avemarías y gloria.
        assertEquals(
            listOf(
                "senal", "contricion", "padrenuestro-inicial",
                "avemaria-inicial-1", "avemaria-inicial-2", "avemaria-inicial-3",
                "gloria-inicial"
            ),
            pasos.take(7).map { it.id }
        )

        // Cierre: letanías, oración final, padrenuestro, avemaría, gloria, salve y jaculatoria.
        assertEquals(
            listOf(
                "letanias", "oracion-final", "padrenuestro-papa",
                "avemaria-final", "gloria-final", "salve", "jaculatoria", "completado"
            ),
            pasos.takeLast(8).map { it.id }
        )
    }

    @Test
    fun generarPasosRosario_buildsFiveCompleteDecades() {
        val pasos = generarPasosRosario(lunes)

        for (misterio in 0 until 5) {
            val decena = pasos.subList(7 + misterio * 15, 7 + misterio * 15 + 15)

            assertEquals(
                listOf(
                    "misterio-$misterio", "padrenuestro-$misterio",
                    "avemaria-$misterio-1", "avemaria-$misterio-2", "avemaria-$misterio-3",
                    "avemaria-$misterio-4", "avemaria-$misterio-5", "avemaria-$misterio-6",
                    "avemaria-$misterio-7", "avemaria-$misterio-8", "avemaria-$misterio-9",
                    "avemaria-$misterio-10",
                    "gloria-$misterio", "ohjesus-$misterio", "mariamadre-$misterio"
                ),
                decena.map { it.id }
            )

            // Toda la decena queda marcada con su índice de misterio.
            assertTrue(decena.all { it.mysteryIndex == misterio })
            assertNull(decena.first().hailMaryIndex)
            assertEquals(
                (1..10).toList(),
                decena.mapNotNull { it.hailMaryIndex }
            )
        }
    }

    @Test
    fun generarPasosRosario_titlesUseOrdinalsAndTheMysteryNameOfTheDay() {
        val gozosos = generarPasosRosario(lunes)

        assertEquals("1.º Misterio Gozosos", gozosos[7].title)
        assertEquals("Misterio 1 de 5 · Gozosos", gozosos[7].subtitle)
        assertEquals("5.º Misterio Gozosos", gozosos[7 + 4 * 15].title)
        assertEquals("Misterio 5 de 5 · Gozosos", gozosos[7 + 4 * 15].subtitle)
        assertEquals("1ª Avemaría", gozosos[3].title)
        assertEquals("10ª Avemaría", gozosos[7 + 11].title)
        assertEquals(
            "Por las intenciones del Santo Padre",
            gozosos[2].subtitle
        )
        assertEquals(
            "Por las intenciones del Santo Padre",
            gozosos.first { it.id == "padrenuestro-papa" }.subtitle
        )

        val jueves = LocalDate.of(2026, 10, 15)
        assertEquals("1.º Misterio Luminosos", generarPasosRosario(jueves)[7].title)

        val domingo = LocalDate.of(2026, 10, 18)
        assertEquals("1.º Misterio Gloriosos", generarPasosRosario(domingo)[7].title)
    }

    @Test
    fun generarPasosRosario_everyStepHasTitleAndText() {
        val pasos = generarPasosRosario(lunes)

        assertTrue(pasos.all { it.id.isNotBlank() })
        assertTrue(pasos.all { it.title.isNotBlank() })
        assertTrue(pasos.all { it.text.isNotBlank() })
    }

    @Test
    fun generarPasosRosario_litanyAvoidsRepeatedInvocationsAndUsesTheAppCrossGlyph() {
        val letanias = generarPasosRosario(lunes).first { it.id == "letanias" }.text

        // La RN repetía estas cuatro invocaciones seguidas; en el port se dejaron una sola vez.
        assertEquals(1, letanias.veces("Salud de los enfermos"))
        assertEquals(1, letanias.veces("Refugio de los pecadores"))
        assertEquals(1, letanias.veces("Consuelo de los migrantes"))
        assertEquals(1, letanias.veces("Consoladora de los afligidos"))

        // La cruz se dibuja con ✝ (el glifo de la app) y no con el 「十」 CJK de la RN.
        assertFalse(letanias.contains("十"))
        assertTrue(letanias.contains("✝ ruega por nosotros."))
        assertTrue(letanias.contains("Cordero de Dios, que quitas el pecado del mundo"))
    }
}
