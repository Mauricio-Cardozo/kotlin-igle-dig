package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.IglesiaDatabaseHelper
import com.example.data.repository.LiturgiaRepository
import com.example.util.detectSeason
import com.example.util.isSunday
import com.example.util.parseWeekNumber
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiturgiaRepositoryTest {

    private lateinit var context: Context
    private lateinit var dbHelper: IglesiaDatabaseHelper
    private lateinit var repository: LiturgiaRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Ensure assets/iglesia_digital.db is copied to database directory
        val dbFile = context.getDatabasePath(IglesiaDatabaseHelper.DATABASE_NAME)
        dbFile.parentFile?.mkdirs()
        if (!dbFile.exists()) {
            context.assets.open(IglesiaDatabaseHelper.DATABASE_NAME).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        dbHelper = IglesiaDatabaseHelper(context)
        repository = LiturgiaRepository(dbHelper)
    }

    @Test
    fun testFecha2026_10_11_DomingoXXVIII() = runBlocking {
        val fecha = "2026-10-11"
        val lectura = repository.getLecturaDelDia(fecha)
        assertNotNull(lectura)
        assertEquals("XXVIII Domingo ordinario", lectura!!.tituloMisa)

        val season = detectSeason(lectura.tituloMisa!!)
        assertEquals("ordinario", season)

        val week = parseWeekNumber(lectura.tituloMisa!!)
        assertEquals(28, week)

        assertTrue(isSunday(lectura.tituloMisa!!))

        val propio = repository.getMisalPropioPorSemana(season!!, week)
        assertNotNull(propio)
        assertEquals(284L, propio!!.id)
        assertEquals("XXVIII DOMINGO DEL TIEMPO ORDINARIO", propio.dia)

        // Verificaciones adicionales de evangelio
        assertNotNull(lectura.evangelio)
        assertTrue(lectura.evangelio!!.startsWith("Mateo 22, 1-10"))
        assertEquals("Lectura del santo evangelio según san Mateo", lectura.evangelioRef)
    }

    @Test
    fun testFecha2026_10_12_LunesXXVIII() = runBlocking {
        val fecha = "2026-10-12"
        val lectura = repository.getLecturaDelDia(fecha)
        assertNotNull(lectura)
        assertEquals("Lunes de la XXVIII semana del Tiempo ordinario", lectura!!.tituloMisa)

        val season = detectSeason(lectura.tituloMisa!!)
        assertEquals("ordinario", season)

        val week = parseWeekNumber(lectura.tituloMisa!!)
        assertEquals(28, week)

        assertFalse(isSunday(lectura.tituloMisa!!))

        // El propio es el mismo del domingo (id 284)
        val propio = repository.getMisalPropioPorSemana(season!!, week)
        assertNotNull(propio)
        assertEquals(284L, propio!!.id)

        // Verificaciones adicionales de evangelio
        assertNotNull(lectura.evangelio)
        assertTrue(lectura.evangelio!!.startsWith("Lucas 11, 29-32"))
        assertNull(lectura.evangelioRef)
    }

    @Test
    fun testFecha2026_10_31_SabadoXXX_y_Santo() = runBlocking {
        val fecha = "2026-10-31"
        val lectura = repository.getLecturaDelDia(fecha)
        assertNotNull(lectura)
        assertEquals("Sábado de la XXX semana del Tiempo ordinario", lectura!!.tituloMisa)

        val season = detectSeason(lectura.tituloMisa!!)
        assertEquals("ordinario", season)

        val week = parseWeekNumber(lectura.tituloMisa!!)
        assertEquals(30, week)

        assertFalse(isSunday(lectura.tituloMisa!!))

        val propio = repository.getMisalPropioPorSemana(season!!, week)
        assertNotNull(propio)
        assertEquals(286L, propio!!.id)
        assertEquals("XXX DOMINGO DEL TIEMPO ORDINARIO", propio.dia)

        // Santo del 31 de Octubre (mes 10, dia 31)
        val santos = repository.getSantosDelDia(10, 31)
        assertEquals(1, santos.size)
        assertEquals("S. Alfonso Rodríguez, religioso jesuita", santos[0].nombre)
    }

    @Test
    fun testFecha2026_01_01_Solemnidad() = runBlocking {
        val fecha = "2026-01-01"
        val lectura = repository.getLecturaDelDia(fecha)
        assertNotNull(lectura)
        assertEquals("Solemnidad de María Santísima, Madre de Dios", lectura!!.tituloMisa)

        val season = detectSeason(lectura.tituloMisa!!)
        assertNull("La solemnidad no contiene palabras clave de temporada directa", season)

        val week = parseWeekNumber(lectura.tituloMisa!!)
        assertEquals(0, week)

        assertFalse(isSunday(lectura.tituloMisa!!))

        // El fallback tampoco encuentra temporada en las adyacentes
        val fallback = repository.getSeasonFromNearestLectura(fecha)
        assertNull(fallback)

        val propio = if (season != null) repository.getMisalPropioPorSemana(season, week) else null
        assertNull(propio)
    }

    @Test
    fun testLecturasDelMesOctubre2026() = runBlocking {
        val lecturas = repository.getLecturasDelMes(2026, 10)
        // Octubre 2026 tiene 31 días y todos tienen lectura en el rango del asset.
        assertEquals(31, lecturas.size)
        assertEquals("2026-10-01", lecturas.first().fecha)
        assertEquals("2026-10-31", lecturas.last().fecha)
        assertTrue("todas las fechas deben traer título de misa", lecturas.all { !it.tituloMisa.isNullOrBlank() })
        assertTrue("debe haber al menos un día con evangelio", lecturas.any { it.tieneEvangelio })

        val dia11 = lecturas.first { it.fecha == "2026-10-11" }
        assertEquals("XXVIII Domingo ordinario", dia11.tituloMisa)
        assertTrue(dia11.tieneEvangelio)
    }

    @Test
    fun testMisalSantosDelDia() = runBlocking {
        // 4 de octubre: San Francisco de Asís (presente en misal_santos).
        val santos = repository.getMisalSantosDelDia(10, 4)
        assertEquals(1, santos.size)
        val francisco = santos.first()
        assertTrue("nombre inesperado: ${francisco.nombre}", francisco.nombre.contains("Francisco"))
        assertNotNull("el propio debe traer colecta", francisco.colecta)

        // Un día sin propio devuelve lista vacía (no falla).
        val vacio = repository.getMisalSantosDelDia(2, 30)
        assertTrue(vacio.isEmpty())
    }

    @Test
    fun testFechaSinLecturaFueraDeRango() = runBlocking {
        val fecha = "2025-12-31" // outside DB range (2026-01-01..2026-12-02)
        val lectura = repository.getLecturaDelDia(fecha)
        assertNull("No hay lectura para $fecha en la base", lectura)

        val season = lectura?.tituloMisa?.let { detectSeason(it) }
        assertNull(season)

        val propio = if (season != null) repository.getMisalPropioPorSemana(season, 0) else null
        assertNull(propio)
    }
}
