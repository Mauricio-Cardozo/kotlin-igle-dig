package com.example

import com.example.ui.navigation.LiturgiaDestinations
import com.example.ui.navigation.OracionDestinations
import com.example.ui.oracion.SECCIONES_ORACION
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Paridad de las secciones del hub de Oración con `ORACIONES_SECCIONES` de la RN
 * (`app/(tabs)/oracion.tsx`): orden, textos, íconos y rutas.
 */
class OracionHubSectionsTest {

    @Test
    fun secciones_matchTheRnHub_inOrderTextsAndIcons() {
        assertEquals(
            listOf("rosario", "coronilla", "favoritos", "oraciones", "jaculatorias", "novenas"),
            SECCIONES_ORACION.map { it.id }
        )
        assertEquals(
            listOf("Santo Rosario", "Coronilla", "Favoritos", "Oraciones", "Jaculatorias", "Novenas"),
            SECCIONES_ORACION.map { it.titulo }
        )
        assertEquals(
            listOf(
                "Guía completa con misterios del día",
                "Divina Misericordia",
                "Versículos, numerales y lecturas guardadas",
                "Del Vaticano: Padre nuestro, Ave María, Credo y más",
                "Oraciones breves para el día",
                "18 devociones de 9 días"
            ),
            SECCIONES_ORACION.map { it.subtitulo }
        )
        assertEquals(
            listOf("📿", "🌿", "❤️", "📖", "🔥", "🕯️"),
            SECCIONES_ORACION.map { it.icono }
        )
    }

    @Test
    fun secciones_routes_matchTheRegisteredDestinations() {
        assertEquals(LiturgiaDestinations.ROSARIO_GUIA, SECCIONES_ORACION[0].ruta)
        assertEquals(LiturgiaDestinations.ROSARIO_CORONILLA, SECCIONES_ORACION[1].ruta)
        assertEquals(OracionDestinations.FAVORITOS, SECCIONES_ORACION[2].ruta)
        assertEquals(OracionDestinations.ORACIONES, SECCIONES_ORACION[3].ruta)
        assertEquals(OracionDestinations.JACULATORIAS, SECCIONES_ORACION[4].ruta)
        assertEquals(OracionDestinations.NOVENAS, SECCIONES_ORACION[5].ruta)
    }
}
