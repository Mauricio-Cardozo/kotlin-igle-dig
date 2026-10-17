package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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
 *
 * También cubre lo que Robolectric no puede: la búsqueda insensible a acentos, los prefijos
 * y la recreación del índice viejo (tokenizador `simple`) en instalaciones existentes.
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

    @Test
    fun search_isAccentAndCaseInsensitive() = runBlocking {
        dbHelper.initializeDatabase()

        val conAcento = repository.searchVersiculos("Jesús")
        val sinAcento = repository.searchVersiculos("jesus")
        assertFalse("jesus no devolvió resultados", sinAcento.isEmpty())
        assertEquals(
            "jesus y Jesús deben devolver exactamente lo mismo",
            conAcento.map { it.id },
            sinAcento.map { it.id }
        )

        val corazon = repository.searchVersiculos("corazon")
        val corazonConAcento = repository.searchVersiculos("corazón")
        assertFalse("corazon no devolvió resultados", corazon.isEmpty())
        assertEquals(corazonConAcento.map { it.id }, corazon.map { it.id })

        assertFalse(
            "senor no devolvió resultados",
            repository.searchVersiculos("senor").isEmpty()
        )
    }

    @Test
    fun search_findsPrefixes() = runBlocking {
        dbHelper.initializeDatabase()

        // "amore" no es una palabra real: solo un prefijo ("amore*") puede encontrar "amores".
        val resultados = repository.searchVersiculos("amore")

        assertFalse("la búsqueda por prefijo no encontró 'amores'", resultados.isEmpty())
    }

    @Test
    fun initializeDatabase_recreatesIndexWithOldTokenizer() = runBlocking {
        // Simula una instalación existente: la base ya está copiada y su FTS fue creado con
        // el tokenizador `simple` (el default, sensible a acentos) por una versión anterior.
        dbHelper.initializeDatabase()

        val dbFile = context.getDatabasePath(IglesiaDatabaseHelper.DATABASE_NAME)
        val raw = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        raw.execSQL("DROP TABLE IF EXISTS biblia_pueblo_dios_fts")
        raw.execSQL(
            "CREATE VIRTUAL TABLE biblia_pueblo_dios_fts " +
                "USING fts4(libro, texto, content='biblia_pueblo_dios')"
        )
        raw.close()

        // El arranque de la app debe detectar el tokenizador viejo y recrear el índice.
        val arranque = IglesiaDatabaseHelper(context)
        arranque.initializeDatabase()

        arranque.getReadableDb().rawQuery(
            "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'biblia_pueblo_dios_fts'",
            null
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            val sql = cursor.getString(0)
            assertTrue(
                "el índice no se recreó con unicode61: $sql",
                sql.contains("remove_diacritics=1")
            )
        }

        val sinAcento = repository.searchVersiculos("jesus")
        assertFalse("tras recrear el índice, jesus no devolvió resultados", sinAcento.isEmpty())
        val conAcento = repository.searchVersiculos("Jesús")
        assertEquals(
            "tras recrear el índice, jesus y Jesús deben devolver lo mismo",
            conAcento.map { it.id },
            sinAcento.map { it.id }
        )
    }
}
