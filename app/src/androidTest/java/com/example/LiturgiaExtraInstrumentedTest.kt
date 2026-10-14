package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.db.IglesiaDatabaseHelper
import com.example.data.repository.LiturgiaRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fase 0.3 (verificación en dispositivo): métodos de datos usados por el Calendario y el
 * detalle del Santo (`getLecturasDelMes`, `getMisalSantosDelDia`).
 */
@RunWith(AndroidJUnit4::class)
class LiturgiaExtraInstrumentedTest {

    private lateinit var context: Context
    private lateinit var dbHelper: IglesiaDatabaseHelper
    private lateinit var repository: LiturgiaRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dbHelper = IglesiaDatabaseHelper(context)
        runBlocking { dbHelper.initializeDatabase() }
        repository = LiturgiaRepository(dbHelper)
    }

    @Test
    fun lecturasDelMes_octubre2026_tiene31Dias() = runBlocking {
        val lecturas = repository.getLecturasDelMes(2026, 10)
        assertEquals(31, lecturas.size)
        assertEquals("2026-10-01", lecturas.first().fecha)
        assertEquals("2026-10-31", lecturas.last().fecha)
        assertTrue(lecturas.any { it.tieneEvangelio })
    }

    @Test
    fun misalSantosDelDia_4deOctubre_esSanFrancisco() = runBlocking {
        val santos = repository.getMisalSantosDelDia(10, 4)
        assertFalse(santos.isEmpty())
        assertTrue(santos.first().nombre.contains("Francisco"))
    }
}
