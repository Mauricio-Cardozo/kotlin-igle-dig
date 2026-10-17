package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.db.IglesiaDatabaseHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verificación en dispositivo: la inicialización crea los índices
 * (libro, capitulo, versiculo) de las dos tablas bíblicas. Como se crean con
 * IF NOT EXISTS durante initializeDatabase(), también quedan para instalaciones que ya
 * tenían la base copiada.
 */
@RunWith(AndroidJUnit4::class)
class BibliaIndexesInstrumentedTest {

    @Test
    fun initializeDatabase_createsBibleIndexes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbHelper = IglesiaDatabaseHelper(context)
        dbHelper.initializeDatabase()

        val indexes = mutableSetOf<String>()
        dbHelper.getReadableDb().rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'index'",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) indexes.add(cursor.getString(0))
        }

        assertTrue("falta idx_biblia_pueblo_dios_lcv", indexes.contains("idx_biblia_pueblo_dios_lcv"))
        assertTrue("falta idx_biblia_navarra_lcv", indexes.contains("idx_biblia_navarra_lcv"))
    }
}
