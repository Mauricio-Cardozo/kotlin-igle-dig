package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.db.BibliaVersion
import com.example.data.db.IglesiaDatabaseHelper
import com.example.data.repository.BibliaRepository
import com.example.data.user.UserDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fase 0.2 (verificación en dispositivo): la versión Navarra es consultable y los ids de
 * favoritos se versionan (`biblia-pueblo-<id>` / `biblia-navarra-<id>`).
 */
@RunWith(AndroidJUnit4::class)
class BibliaVersionInstrumentedTest {

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
    fun navarra_hasBooksAndSearchResults() = runBlocking {
        dbHelper.initializeDatabase()

        val puebloLibros = repository.getLibros(BibliaVersion.PUEBLO)
        val navarraLibros = repository.getLibros(BibliaVersion.NAVARRA)
        assertFalse("Pueblo de Dios sin libros", puebloLibros.isEmpty())
        assertFalse("Navarra sin libros", navarraLibros.isEmpty())

        val navarraResults = repository.searchVersiculos("Dios", BibliaVersion.NAVARRA)
        assertFalse("búsqueda en Navarra sin resultados", navarraResults.isEmpty())
    }

    @Test
    fun toggleFavorito_createsVersionedIds() = runBlocking {
        dbHelper.initializeDatabase()

        val puebloVersiculo = repository.getVersiculos("Génesis", 1, BibliaVersion.PUEBLO).first()
        repository.toggleFavorito(puebloVersiculo, "#55C9A84C", BibliaVersion.PUEBLO)
        val puebloId = "${BibliaVersion.PUEBLO.favoritoPrefix}${puebloVersiculo.id}"
        assertNotNull("falta el favorito versionado de Pueblo", repository.getFavorito(puebloId))
        assertNull("no debe existir la forma antigua", repository.getFavorito("biblia-${puebloVersiculo.id}"))

        val navarraVersiculo = repository.getVersiculos("Génesis", 1, BibliaVersion.NAVARRA).first()
        repository.toggleFavorito(navarraVersiculo, "#55C9A84C", BibliaVersion.NAVARRA)
        val navarraId = "${BibliaVersion.NAVARRA.favoritoPrefix}${navarraVersiculo.id}"
        assertNotNull("falta el favorito versionado de Navarra", repository.getFavorito(navarraId))
        assertTrue("los ids de ambas versiones deben diferir", puebloId != navarraId)
    }
}
