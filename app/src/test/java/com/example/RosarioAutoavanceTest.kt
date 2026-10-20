package com.example

import com.example.ui.oracion.Autoavance
import com.example.ui.oracion.RosarioStep
import com.example.ui.oracion.duracionPasoSegundos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RosarioAutoavanceTest {

    private fun paso(texto: String) = RosarioStep("test", "Test", texto)

    private val gloria = paso(
        "Gloria al Padre, y al Hijo, y al Espíritu Santo. Como era en el principio, " +
            "ahora y siempre, por los siglos de los siglos. Amén."
    )
    private val letanias = paso((1..200).joinToString(" ") { "Señor ten piedad de nosotros" })

    @Test
    fun duracion_apagadoNoTieneTiempo() {
        assertEquals(0, duracionPasoSegundos(gloria, Autoavance.APAGADO))
    }

    @Test
    fun duracion_lentoNormalYRapidoSeOrdenanPorVelocidad() {
        val lento = duracionPasoSegundos(gloria, Autoavance.LENTO)
        val normal = duracionPasoSegundos(gloria, Autoavance.NORMAL)
        val rapido = duracionPasoSegundos(gloria, Autoavance.RAPIDO)

        assertTrue("lento ($lento) > normal ($normal)", lento > normal)
        assertTrue("normal ($normal) > rápido ($rapido)", normal > rapido)
    }

    @Test
    fun duracion_creceConLaLongitudDelTexto() {
        val corto = duracionPasoSegundos(gloria, Autoavance.NORMAL)
        val largo = duracionPasoSegundos(letanias, Autoavance.NORMAL)

        assertTrue("letanías ($largo s) > Gloria ($corto s)", largo > corto)
    }

    @Test
    fun duracion_seAcotaEntreCincoSegundosYCuatroMinutos() {
        assertEquals(5, duracionPasoSegundos(paso(""), Autoavance.RAPIDO))

        val inmenso = paso((1..10_000).joinToString(" ") { "amén" })
        assertEquals(240, duracionPasoSegundos(inmenso, Autoavance.LENTO))
    }

    @Test
    fun duracion_esRazonableParaUnAvemaria() {
        val avemaria = paso(
            "Dios te salve, María, llena eres de gracia, el Señor es contigo. Bendita tú eres " +
                "entre todas las mujeres, y bendito es el fruto de tu vientre, Jesús. Santa " +
                "María, Madre de Dios, ruega por nosotros, pecadores, ahora y en la hora de " +
                "nuestra muerte. Amén."
        )

        val segundos = duracionPasoSegundos(avemaria, Autoavance.NORMAL)
        assertTrue("un avemaría tarda $segundos s", segundos in 20..35)
    }
}
