package com.example

import com.example.ui.biblia.GRUPOS_BIBLIA
import com.example.ui.biblia.abreviatura
import com.example.ui.biblia.testamentoDe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifica que el catálogo canónico portado de la RN (73 libros) mantenga las
 * mismas 9 secciones, conteos por testamento y deuterocanónicos.
 */
class BibliaCatalogTest {

    @Test
    fun catalogo_tieneNueveSeccionesY73Libros() {
        assertEquals(9, GRUPOS_BIBLIA.size)
        assertEquals(73, GRUPOS_BIBLIA.sumOf { it.libros.size })
    }

    @Test
    fun catalogo_cuenta46AntiguosY27Nuevos() {
        assertEquals(46, GRUPOS_BIBLIA.filter { it.testamento == "Antiguo" }.sumOf { it.libros.size })
        assertEquals(27, GRUPOS_BIBLIA.filter { it.testamento == "Nuevo" }.sumOf { it.libros.size })
    }

    @Test
    fun catalogo_marcaLosNueveDeuterocanonicos() {
        val deuterocanonicos = GRUPOS_BIBLIA
            .flatMap { it.libros }
            .filter { it.deuterocanonico }
            .map { it.libro }
            .toSet()
        assertEquals(
            setOf(
                "Tobías", "Judit", "Ester", "1 Macabeos", "2 Macabeos",
                "Sabiduría", "Eclesiástico", "Baruc", "Daniel"
            ),
            deuterocanonicos
        )
    }

    @Test
    fun abreviaturas_cubrenLibrosRepresentativos() {
        assertEquals("Gn", abreviatura("Génesis"))
        assertEquals("1 Cor", abreviatura("1 Corintios"))
        assertEquals("Hch", abreviatura("Hechos de los Apóstoles"))
        assertEquals("Ap", abreviatura("Apocalipsis"))
        // Fallback de la RN para libros fuera del mapa.
        assertEquals("Lib", abreviatura("LibroDesconocido"))
    }

    @Test
    fun testamentoDe_resuelveAmbosTestamentos() {
        assertEquals("Antiguo", testamentoDe("Génesis"))
        assertEquals("Nuevo", testamentoDe("Apocalipsis"))
        assertNull(testamentoDe("NoExiste"))
    }
}
