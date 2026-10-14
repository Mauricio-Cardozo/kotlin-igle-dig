package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.db.IglesiaDatabaseHelper
import com.example.data.repository.BibliaRepository
import com.example.data.user.UserDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fase 0.1 (verificación en dispositivo): la SQLite real de Android trae FTS4 pero no FTS5.
 * Comprueba que initializeDatabase() crea las tablas FTS (pueblo + navarra), que no pisa el
 * PRAGMA user_version del asset (10) y que searchVersiculos usa el índice.
 */
@RunWith(AndroidJUnit4::class)
class BibliaFtsInstrumentedTest {

    private lateinit var context: Context
    private lateinit var dbHelper: IglesiaDatabaseHelper
    private lateinit var userDb: UserDatabase
    private lateinit var repository: BibliaRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dbHelper = IglesiaDatabaseHelper(context)
        userDb = Room.inMemoryDatabaseBuilder(context, UserDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BibliaRepository(dbHelper, userDb.favoritoDao())
    }

    @After
    fun tearDown() {
        userDb.close()
    }

    @Test
    fun initializeDatabase_buildsBibleFtsTables_andKeepsUserVersion() = runBlocking {
        dbHelper.initializeDatabase()
        val db = dbHelper.getReadableDb()

        val tables = mutableSetOf<String>()
        db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null).use { cursor ->
            while (cursor.moveToNext()) tables.add(cursor.getString(0))
        }

        assertTrue("falta biblia_pueblo_dios_fts", tables.contains("biblia_pueblo_dios_fts"))
        assertTrue("falta biblia_navarra_fts", tables.contains("biblia_navarra_fts"))

        db.rawQuery("PRAGMA user_version", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(10, cursor.getInt(0))
        }
    }

    @Test
    fun searchVersiculos_usesFts_andReturnsResults() = runBlocking {
        dbHelper.initializeDatabase()

        val results = repository.searchVersiculos("Jesús")

        assertFalse("la búsqueda FTS no devolvió resultados", results.isEmpty())
        assertTrue("se superó el límite de 50", results.size <= 50)
        assertTrue("hay resultados sin texto", results.all { it.texto.isNotBlank() })
    }
}
