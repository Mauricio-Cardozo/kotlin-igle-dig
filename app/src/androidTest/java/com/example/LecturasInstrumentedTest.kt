package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.db.IglesiaDatabaseHelper
import com.example.data.repository.LiturgiaRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Hito 2: cobertura y calidad de la tabla `lecturas` (regenerada con los scrapers
 * de `tools/lecturas/`) y degradación elegante fuera del rango de datos.
 */
@RunWith(AndroidJUnit4::class)
class LecturasInstrumentedTest {

    private lateinit var dbHelper: IglesiaDatabaseHelper
    private lateinit var repository: LiturgiaRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dbHelper = IglesiaDatabaseHelper(context)
        repository = LiturgiaRepository(dbHelper)
        runBlocking { dbHelper.initializeDatabase() }
    }

    @Test
    fun lecturas_cubrenHasta2027() {
        val db = dbHelper.getReadableDb()
        db.rawQuery("SELECT MAX(fecha) FROM lecturas", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            val max = cursor.getString(0)
            assertTrue("la cobertura quedó corta: $max", max >= "2027-01-01")
        }
    }

    @Test
    fun lecturas_futurasTienenEvangelio() {
        val db = dbHelper.getReadableDb()
        db.rawQuery(
            "SELECT COUNT(*) FROM lecturas WHERE fecha >= '2027-01-01' " +
                "AND evangelio IS NOT NULL AND evangelio != ''",
            null
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue("faltan lecturas de 2027 con evangelio", cursor.getInt(0) >= 2)
        }
    }

    @Test
    fun lecturas_domingo_tieneSegundaSeparadaDeLaPrimera() {
        val db = dbHelper.getReadableDb()
        db.rawQuery(
            "SELECT primera_lectura, segunda_lectura FROM lecturas WHERE fecha = '2026-10-18'",
            null
        ).use { cursor ->
            assertTrue("no está el domingo 2026-10-18", cursor.moveToFirst())
            val primera = cursor.getString(0) ?: ""
            val segunda = cursor.getString(1) ?: ""
            assertFalse("la 2ª lectura sigue pegada a la 1ª", primera.contains("Segunda lectura"))
            assertTrue("falta la 2ª lectura del domingo", segunda.isNotBlank())
        }
    }

    @Test
    fun lecturas_fueraDeRango_noRevientaYDevuelveNull() = runBlocking {
        assertNull(repository.getLecturaDelDia("2030-01-01"))
    }
}
