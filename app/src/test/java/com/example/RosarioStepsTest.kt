package com.example

import com.example.ui.oracion.IdiomaRosario
import com.example.ui.oracion.MisterioRosario
import com.example.ui.oracion.OracionesRosario
import com.example.ui.oracion.SerieMisterios
import com.example.ui.oracion.generarPasosRosario
import com.example.ui.oracion.misteriosDelDia
import com.example.ui.oracion.textosDe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RosarioStepsTest {

    /** Lunes 12/10/2026: misterios Gozosos. */
    private val lunes = LocalDate.of(2026, 10, 12)

    /** Cuenta las apariciones no solapadas de [aguja] en el texto. */
    private fun String.veces(aguja: String): Int = split(aguja).size - 1

    @Test
    fun misteriosDelDia_matchesTheRnWeekdayAssignmentInOrdinaryTime() {
        // Semana del 12 al 18 de octubre de 2026: tiempo ordinario, sin excepciones litúrgicas.
        val esperado = mapOf(
            LocalDate.of(2026, 10, 12) to SerieMisterios.GOZOSOS,   // lunes
            LocalDate.of(2026, 10, 13) to SerieMisterios.DOLOROSOS, // martes
            LocalDate.of(2026, 10, 14) to SerieMisterios.GLORIOSOS, // miércoles
            LocalDate.of(2026, 10, 15) to SerieMisterios.LUMINOSOS, // jueves
            LocalDate.of(2026, 10, 16) to SerieMisterios.DOLOROSOS, // viernes
            LocalDate.of(2026, 10, 17) to SerieMisterios.GOZOSOS,   // sábado
            LocalDate.of(2026, 10, 18) to SerieMisterios.GLORIOSOS  // domingo
        )
        val textos = textosDe(IdiomaRosario.ESPANOL)

        esperado.forEach { (fecha, serie) ->
            assertEquals("Misterios del $fecha", serie, misteriosDelDia(fecha))
            assertEquals(5, textos.misteriosDe(serie).size)
            assertTrue(textos.misteriosDe(serie).all { it.isNotBlank() })
        }
    }

    @Test
    fun misteriosDelDia_usesJoyfulMysteriesOnChristmasAndAdventSundays() {
        // Navidad de 2026 cae viernes; igual se rezan los Gozosos.
        assertEquals(SerieMisterios.GOZOSOS, misteriosDelDia(LocalDate.of(2026, 12, 25)))
        // Primer y segundo domingo de Adviento.
        assertEquals(SerieMisterios.GOZOSOS, misteriosDelDia(LocalDate.of(2026, 11, 29)))
        assertEquals(SerieMisterios.GOZOSOS, misteriosDelDia(LocalDate.of(2026, 12, 6)))
        // El domingo después de Navidad vuelve a la asignación semanal.
        assertEquals(SerieMisterios.GLORIOSOS, misteriosDelDia(LocalDate.of(2026, 12, 27)))
    }

    @Test
    fun misteriosDelDia_usesSorrowfulMysteriesOnLentSundaysUntilPalmSunday() {
        // Miércoles de Ceniza (18/02/2026): día de semana, asignación normal.
        assertEquals(SerieMisterios.GLORIOSOS, misteriosDelDia(LocalDate.of(2026, 2, 18)))
        // Primer domingo de Cuaresma (22/02/2026): la regla de Rosarium se lo saltaba.
        assertEquals(SerieMisterios.DOLOROSOS, misteriosDelDia(LocalDate.of(2026, 2, 22)))
        // Domingo de Ramos (29/03/2026).
        assertEquals(SerieMisterios.DOLOROSOS, misteriosDelDia(LocalDate.of(2026, 3, 29)))
        // Domingo de Pascua (05/04/2026).
        assertEquals(SerieMisterios.GLORIOSOS, misteriosDelDia(LocalDate.of(2026, 4, 5)))
    }

    @Test
    fun misteriosDelDia_manualSelectionOverridesTheLiturgicalRule() {
        // Domingo de Pascua en Automático: Gloriosos; fijado a mano manda la selección.
        val pascua = LocalDate.of(2026, 4, 5)
        assertEquals(SerieMisterios.GLORIOSOS, misteriosDelDia(pascua))
        assertEquals(SerieMisterios.DOLOROSOS, misteriosDelDia(pascua, MisterioRosario.DOLOROSOS))
        assertEquals(SerieMisterios.GLORIOSOS, misteriosDelDia(pascua, MisterioRosario.GLORIOSOS))

        // Navidad y domingos de Cuaresma también respetan la elección manual.
        assertEquals(
            SerieMisterios.LUMINOSOS,
            misteriosDelDia(LocalDate.of(2026, 12, 25), MisterioRosario.LUMINOSOS)
        )
        assertEquals(
            SerieMisterios.GOZOSOS,
            misteriosDelDia(LocalDate.of(2026, 2, 22), MisterioRosario.GOZOSOS)
        )
    }

    @Test
    fun generarPasosRosario_manualMysteryBuildsItsOwnDecades() {
        val pasos = generarPasosRosario(lunes, MisterioRosario.LUMINOSOS)

        assertEquals(90, pasos.size)
        assertEquals("1.º Misterio Luminosos", pasos[7].title)
        assertEquals("Misterio 1 de 5 · Luminosos", pasos[7].subtitle)
        assertEquals("El Bautismo de Jesús en el Jordán", pasos[7].text)
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
    fun generarPasosRosario_optionalPrayersAreOnByDefaultAndCanBeRemoved() {
        val completos = generarPasosRosario(lunes)
        assertEquals(90, completos.size)
        assertTrue(completos.any { it.id == "ohjesus-0" })
        assertTrue(completos.any { it.id == "salve" })
        assertTrue(completos.any { it.id == "oracion-final" })

        val sinNinguna = generarPasosRosario(
            lunes,
            oraciones = OracionesRosario(fatima = false, salve = false, oracionFinal = false)
        )
        assertEquals(83, sinNinguna.size)
        assertFalse(sinNinguna.any { it.id.startsWith("ohjesus-") })
        assertFalse(sinNinguna.any { it.id == "salve" })
        assertFalse(sinNinguna.any { it.id == "oracion-final" })
        assertEquals("completado", sinNinguna.last().id)

        // Las oraciones que no son opcionales siguen en su sitio.
        assertEquals(5, sinNinguna.count { it.id.startsWith("mariamadre-") })
        assertEquals(1, sinNinguna.count { it.id == "letanias" })
    }

    @Test
    fun generarPasosRosario_eachOptionalPrayerChangesOnlyItsOwnSteps() {
        assertEquals(85, generarPasosRosario(lunes, oraciones = OracionesRosario(fatima = false)).size)
        assertEquals(89, generarPasosRosario(lunes, oraciones = OracionesRosario(salve = false)).size)
        assertEquals(89, generarPasosRosario(lunes, oraciones = OracionesRosario(oracionFinal = false)).size)

        // Sin Fátima, la decena sigue con Gloria y María, Madre de Gracia.
        val sinFatima = generarPasosRosario(lunes, oraciones = OracionesRosario(fatima = false))
        assertEquals(
            listOf(
                "misterio-0", "padrenuestro-0",
                "avemaria-0-1", "avemaria-0-2", "avemaria-0-3", "avemaria-0-4",
                "avemaria-0-5", "avemaria-0-6", "avemaria-0-7", "avemaria-0-8",
                "avemaria-0-9", "avemaria-0-10",
                "gloria-0", "mariamadre-0"
            ),
            sinFatima.subList(7, 7 + 14).map { it.id }
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

        // Las excepciones litúrgicas también cambian el nombre en los pasos.
        assertEquals(
            "1.º Misterio Gozosos",
            generarPasosRosario(LocalDate.of(2026, 11, 29))[7].title
        )
        assertEquals(
            "1.º Misterio Dolorosos",
            generarPasosRosario(LocalDate.of(2026, 2, 22))[7].title
        )
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

    @Test
    fun idiomaRosario_desdeClave_fallsBackToSpanish() {
        assertEquals(IdiomaRosario.ESPANOL, IdiomaRosario.desdeClave("es"))
        assertEquals(IdiomaRosario.LATIN, IdiomaRosario.desdeClave("la"))
        assertEquals(IdiomaRosario.ESPANOL, IdiomaRosario.desdeClave("fr"))
        assertEquals(IdiomaRosario.ESPANOL, IdiomaRosario.desdeClave(null))
    }

    @Test
    fun generarPasosRosario_keepsTheSameIdsAndOrderInSpanishAndLatin() {
        // Las ocho combinaciones de oraciones opcionales: el idioma no altera la secuencia.
        val combinaciones = (0 until 8).map { bits ->
            OracionesRosario(
                fatima = (bits and 1) != 0,
                salve = (bits and 2) != 0,
                oracionFinal = (bits and 4) != 0
            )
        }

        for (oraciones in combinaciones) {
            val espanol = generarPasosRosario(
                lunes,
                oraciones = oraciones,
                idioma = IdiomaRosario.ESPANOL
            )
            val latin = generarPasosRosario(
                lunes,
                oraciones = oraciones,
                idioma = IdiomaRosario.LATIN
            )

            assertEquals(espanol.size, latin.size)
            assertEquals(espanol.map { it.id }, latin.map { it.id })
            assertEquals(espanol.map { it.mysteryIndex }, latin.map { it.mysteryIndex })
            assertEquals(espanol.map { it.hailMaryIndex }, latin.map { it.hailMaryIndex })
        }
    }

    @Test
    fun generarPasosRosario_latinTextsAreCompleteAndUseTheirOwnPrayers() {
        val latin = generarPasosRosario(lunes, idioma = IdiomaRosario.LATIN)

        assertTrue(latin.all { it.title.isNotBlank() && it.text.isNotBlank() })
        assertFalse(latin.any { it.title.contains("十") || it.text.contains("十") })

        assertEquals("Signum Crucis", latin[0].title)
        assertEquals("Mysterium I Gaudiosum", latin[7].title)
        assertEquals("Mysterium 1 ex 5 · Gaudiosa", latin[7].subtitle)
        assertEquals("Annuntiatio Filii Dei", latin[7].text)
        assertEquals("Ave Maria 1", latin[3].title)
        assertTrue(latin[2].text.contains("Pater noster,"))
        assertTrue(latin.first { it.id == "salve" }.text.contains("Salve, Regina"))
        assertFalse(latin.first { it.id == "salve" }.text.contains("Dios te salve"))
        assertEquals("Iaculatoria Finalis", latin.first { it.id == "jaculatoria" }.title)
    }
}
