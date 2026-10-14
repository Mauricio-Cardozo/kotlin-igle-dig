package com.example

import com.example.data.db.BibliaVersion
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas unitarias puras del enum de versiones bíblicas (Fase 0.2).
 */
class BibliaVersionTest {

    @Test
    fun fromKey_resolvesKnownKeys() {
        assertEquals(BibliaVersion.PUEBLO, BibliaVersion.fromKey("pueblo"))
        assertEquals(BibliaVersion.NAVARRA, BibliaVersion.fromKey("navarra"))
    }

    @Test
    fun fromKey_fallsBackToDefault() {
        assertEquals(BibliaVersion.PUEBLO, BibliaVersion.fromKey(null))
        assertEquals(BibliaVersion.PUEBLO, BibliaVersion.fromKey("desconocida"))
    }

    @Test
    fun tableAndFavoritoNames_matchDatabase() {
        assertEquals("biblia_pueblo_dios", BibliaVersion.PUEBLO.tableName)
        assertEquals("biblia_pueblo_dios_fts", BibliaVersion.PUEBLO.ftsTableName)
        assertEquals("biblia-pueblo-", BibliaVersion.PUEBLO.favoritoPrefix)

        assertEquals("biblia_navarra", BibliaVersion.NAVARRA.tableName)
        assertEquals("biblia_navarra_fts", BibliaVersion.NAVARRA.ftsTableName)
        assertEquals("biblia-navarra-", BibliaVersion.NAVARRA.favoritoPrefix)
    }
}
