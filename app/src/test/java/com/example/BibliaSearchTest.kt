package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.IglesiaDatabaseHelper
import com.example.data.repository.BibliaRepository
import com.example.data.user.UserDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifica en la JVM que la búsqueda bíblica no se rompe y devuelve resultados.
 * La SQLite que usa Robolectric no garantiza tener módulos FTS, así que este test NO
 * comprueba la creación del índice: eso se verifica en dispositivo con
 * BibliaFtsInstrumentedTest (app/src/androidTest).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BibliaSearchTest {

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
    fun search_returnsResults_andNeverThrows() = runBlocking {
        dbHelper.initializeDatabase()

        // "Jesús" es una palabra muy frecuente: debe encontrarse con FTS o con el LIKE.
        val results = repository.searchVersiculos("Jesús")

        assertFalse("la búsqueda no devolvió resultados", results.isEmpty())
        assertTrue("se superó el límite de 50", results.size <= 50)
        assertTrue("hay resultados sin texto", results.all { it.texto.isNotBlank() })
    }
}
