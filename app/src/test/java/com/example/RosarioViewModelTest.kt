package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.user.StreaksPreferencesRepository
import com.example.data.user.UserPreferencesRepository
import com.example.ui.oracion.MisterioRosario
import com.example.ui.oracion.RosarioViewModel
import com.example.util.FixedDateProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RosarioViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    /** Lunes 12/10/2026: misterios Gozosos. */
    private val hoy = LocalDate.of(2026, 10, 12)
    private val fechaProvider = FixedDateProvider(hoy, LocalTime.of(20, 0))

    private lateinit var context: Context
    private lateinit var streaksRepository: StreaksPreferencesRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    @Before
    fun setup() {
        // `Main` unconfined (como el `Main.immediate` de producción) y sin `resetMain` entre
        // tests: los `edit` de DataStore que quedan en vuelo al terminar un test se reanudan
        // con el contexto del llamador; si se restaurara el Main real de Robolectric (looper
        // en pausa) quedarían postados ahí para siempre reteniendo el lock del coordinador y
        // colgarían hasta el `@Before` del test siguiente. Con unconfined la reanudación corre
        // inline en el hilo del actor y el drenado siempre termina. Cada `@Before` reinstala
        // el dispatcher antes de tocar los repos.
        Dispatchers.setMain(UnconfinedTestDispatcher(testDispatcher.scheduler))
        context = ApplicationProvider.getApplicationContext()
        streaksRepository = StreaksPreferencesRepository(context, fechaProvider)
        userPreferencesRepository = UserPreferencesRepository(context)

        // Los DataStore viven en un singleton por proceso y se comparten entre tests del mismo
        // sandbox: se parte de un estado limpio conocido. El reseteo corre sobre IO real para
        // no depender del scheduler del test.
        runBlocking(Dispatchers.IO) {
            streaksRepository.setRosarioStats(null, null, null)
            streaksRepository.guardarRosarioProgreso("", 0)
            userPreferencesRepository.setFontSizeMultiplier(UserPreferencesRepository.DEFAULT_FONT_SCALE)
            userPreferencesRepository.setRosarioMisterioManual(
                UserPreferencesRepository.DEFAULT_ROSARIO_MISTERIO_MANUAL
            )
        }
    }

    private fun crearViewModel(): RosarioViewModel =
        RosarioViewModel(streaksRepository, userPreferencesRepository, fechaProvider)

    private suspend fun RosarioViewModel.awaitLoaded() = uiState.first { !it.isLoading }

    @Test
    fun viewModel_startsAtTheFirstStep_withTheMysteriesOfTheDay() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        val estado = viewModel.awaitLoaded()

        assertFalse(estado.isLoading)
        assertEquals(0, estado.pasoIndex)
        assertEquals(90, estado.total)
        assertEquals(MisterioRosario.AUTOMATICO, estado.misterioManual)
        assertEquals("Señal de la Cruz", estado.paso?.title)
        assertEquals("1.º Misterio Gozosos", estado.pasos[7].title)
    }

    @Test
    fun viewModel_manualMystery_overridesTheDayAndResetsTheSavedStep() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        viewModel.awaitLoaded()
        viewModel.siguiente()
        viewModel.siguiente()
        esperarProgreso(2)

        viewModel.setMisterioManual(MisterioRosario.DOLOROSOS)

        val estado = viewModel.uiState.value
        assertEquals(MisterioRosario.DOLOROSOS, estado.misterioManual)
        assertEquals(0, estado.pasoIndex)
        assertEquals("1.º Misterio Dolorosos", estado.pasos[7].title)
        assertEquals("La Oración de Jesús en el huerto", estado.pasos[7].text)

        // El índice guardado correspondía a la lista anterior: se descarta sin tocar la racha.
        esperarSinProgreso()
        val progreso = streaksRepository.getRosarioProgreso()
        assertNull(progreso.fechaIso)
        assertNull(progreso.paso)
    }

    @Test
    fun viewModel_manualMystery_persistsThePreference() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        viewModel.awaitLoaded()
        viewModel.siguiente()
        esperarProgreso(1)

        viewModel.setMisterioManual(MisterioRosario.LUMINOSOS)

        // La preferencia se guarda después del borrado: esperarla garantiza ambos efectos.
        esperarPreferencia(MisterioRosario.LUMINOSOS)
        assertNull(streaksRepository.getRosarioProgreso().paso)
    }

    @Test
    fun viewModel_manualMystery_isRestoredFromTheSavedPreference() = runTest(testDispatcher) {
        userPreferencesRepository.setRosarioMisterioManual(MisterioRosario.LUMINOSOS.valor)

        val viewModel = crearViewModel()
        val estado = viewModel.awaitLoaded()

        assertEquals(MisterioRosario.LUMINOSOS, estado.misterioManual)
        assertEquals("1.º Misterio Luminosos", estado.pasos[7].title)
    }

    @Test
    fun viewModel_manualMystery_keepsTheCompletionAfterChangingMystery() = runTest(testDispatcher) {
        // Reanuda casi al final en vez de encolar 89 avances: menos escrituras de DataStore.
        streaksRepository.guardarRosarioProgreso(hoy.toString(), 88)

        val viewModel = crearViewModel()
        viewModel.awaitLoaded()
        viewModel.siguiente()
        viewModel.marcarCompletado()
        esperarTotalRosarios(1)

        // Cambiar de misterio después de terminar reinicia el rezo pero no vuelve a contar.
        viewModel.setMisterioManual(MisterioRosario.GLORIOSOS)

        assertEquals(0, viewModel.uiState.value.pasoIndex)
        assertTrue(viewModel.uiState.value.completado)
        assertEquals(1, totalRosarios())
    }

    @Test
    fun viewModel_restoresTheSavedStepOfToday() = runTest(testDispatcher) {
        streaksRepository.guardarRosarioProgreso(hoy.toString(), 12)

        val viewModel = crearViewModel()

        assertEquals(12, viewModel.awaitLoaded().pasoIndex)
    }

    @Test
    fun viewModel_ignoresTheSavedStepOfAnotherDay() = runTest(testDispatcher) {
        streaksRepository.guardarRosarioProgreso("2026-10-11", 12)

        val viewModel = crearViewModel()

        assertEquals(0, viewModel.awaitLoaded().pasoIndex)
    }

    @Test
    fun viewModel_advancesAndGoesBack_withinTheSteps() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        viewModel.awaitLoaded()

        viewModel.siguiente()
        viewModel.siguiente()
        assertEquals(2, viewModel.uiState.value.pasoIndex)

        viewModel.anterior()
        assertEquals(1, viewModel.uiState.value.pasoIndex)

        // No hay pasos antes del primero.
        viewModel.anterior()
        viewModel.anterior()
        assertEquals(0, viewModel.uiState.value.pasoIndex)

        // Tampoco más allá del último: el cierre real es marcarCompletado().
        repeat(100) { viewModel.siguiente() }
        val final = viewModel.uiState.value
        assertEquals(89, final.pasoIndex)
        assertEquals(RosarioViewModel.PASO_COMPLETADO_ID, final.paso?.id)
        assertFalse(final.completado)
    }

    @Test
    fun viewModel_persistsTheStepWhilePraying() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        viewModel.awaitLoaded()

        viewModel.siguiente()
        viewModel.siguiente()

        // El guardado lo hace una corrutina del viewModelScope: se espera a observar el
        // efecto (FIFO en DataStore) en lugar de asumir que ya se escribió.
        esperarProgreso(2)
        val progreso = streaksRepository.getRosarioProgreso()
        assertEquals(hoy.toString(), progreso.fechaIso)
        assertEquals(2, progreso.paso)
    }

    @Test
    fun viewModel_marksCompleted_updatingStreakAndTotals() = runTest(testDispatcher) {
        // Viene de rezar ayer: 4 días de racha y 10 rosarios históricos.
        streaksRepository.setRosarioStats("2026-10-11", "4", "10")
        streaksRepository.guardarRosarioProgreso(hoy.toString(), 88)

        val viewModel = crearViewModel()
        viewModel.awaitLoaded()
        assertEquals(88, viewModel.uiState.value.pasoIndex)

        viewModel.siguiente()
        viewModel.marcarCompletado()
        assertTrue(viewModel.uiState.value.completado)

        val stats = streaksRepository.streakStatsFlow.first { it.rosarioTotal == 11 }
        assertEquals(5, stats.rachaRosarioDias)
        assertEquals("2026-10-12", stats.rosarioUltima)

        // La misma transacción del completado descarta el progreso a medias.
        val progreso = streaksRepository.getRosarioProgreso()
        assertNull(progreso.fechaIso)
        assertNull(progreso.paso)
    }

    @Test
    fun viewModel_completionCountsOncePerRun() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        viewModel.awaitLoaded()
        repeat(100) { viewModel.siguiente() }
        viewModel.marcarCompletado()

        val primera = streaksRepository.streakStatsFlow.first { it.rosarioTotal == 1 }
        assertEquals(1, primera.rachaRosarioDias)

        // "Rezar de nuevo" y volver a terminar no suma otro rosario en la misma corrida.
        viewModel.rezarDeNuevo()
        repeat(100) { viewModel.siguiente() }
        viewModel.marcarCompletado()
        advanceUntilIdle()

        assertEquals(1, streaksRepository.streakStatsFlow.first().rosarioTotal)
    }

    @Test
    fun viewModel_rezarDeNuevo_returnsToStartKeepingTheCompletion() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        viewModel.awaitLoaded()
        repeat(100) { viewModel.siguiente() }
        viewModel.marcarCompletado()

        viewModel.rezarDeNuevo()

        val estado = viewModel.uiState.value
        assertEquals(0, estado.pasoIndex)
        assertTrue(estado.completado)
    }

    @Test
    fun viewModel_onScreenEntered_keepsTheStep_andResetsAfterCompletion() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        viewModel.awaitLoaded()
        viewModel.siguiente()
        viewModel.siguiente()

        // Mismo día y rezo a medias: la sesión sigue donde estaba.
        viewModel.onScreenEntered()
        assertEquals(2, viewModel.uiState.value.pasoIndex)

        // Tras completar, volver a entrar arranca un rezo nuevo (la RN re-monta la pantalla).
        repeat(100) { viewModel.siguiente() }
        viewModel.marcarCompletado()
        viewModel.onScreenEntered()

        val reiniciado = viewModel.uiState.value
        assertEquals(0, reiniciado.pasoIndex)
        assertFalse(reiniciado.completado)
        assertEquals(90, reiniciado.total)
    }

    @Test
    fun viewModel_fontSizeControls_updateThePreference() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        viewModel.awaitLoaded()

        viewModel.increaseFontSize()
        assertEquals(1.1f, viewModel.fontSizeMultiplier.first { it == 1.1f })

        // `decreaseFontSize` calcula desde el valor observado, así que se espera el efecto de
        // cada toque antes del siguiente (como los frames de la UI).
        viewModel.decreaseFontSize()
        assertEquals(1.0f, viewModel.fontSizeMultiplier.first { it == 1.0f })
        viewModel.decreaseFontSize()
        assertEquals(0.9f, viewModel.fontSizeMultiplier.first { it == 0.9f })

        viewModel.resetFontSize()
        assertEquals(1.0f, viewModel.fontSizeMultiplier.first { it == 1.0f })

        assertEquals(1.0f, userPreferencesRepository.fontSizeMultiplier.first { it == 1.0f })
    }

    /** Espera (en tiempo real) a que el progreso guardado refleje [paso], con tope para no colgar el test. */
    private suspend fun esperarProgreso(paso: Int) {
        // El actor de DataStore escribe en hilos reales de IO: un `withTimeout` virtual agota
        // miles de milisegundos simulados en microsegundos reales antes de que la escritura
        // llegue. Se espera con tiempo real, fuera del scheduler del test.
        withContext(Dispatchers.IO) {
            withTimeout(5_000) {
                while (streaksRepository.getRosarioProgreso().paso != paso) delay(1)
            }
        }
    }

    /** Espera (en tiempo real) a que el progreso guardado quede vacío. */
    private suspend fun esperarSinProgreso() {
        withContext(Dispatchers.IO) {
            withTimeout(5_000) {
                while (streaksRepository.getRosarioProgreso().paso != null) delay(1)
            }
        }
    }

    /** Espera (en tiempo real) a que DataStore publique la selección manual de misterios. */
    private suspend fun esperarPreferencia(misterio: MisterioRosario) {
        withContext(Dispatchers.IO) {
            withTimeout(5_000) {
                while (userPreferencesRepository.rosarioMisterioManual.first() != misterio.valor) {
                    delay(1)
                }
            }
        }
    }

    /** Espera (en tiempo real) a que el total histórico de rosarios llegue a [total]. */
    private suspend fun esperarTotalRosarios(total: Int) {
        withContext(Dispatchers.IO) {
            withTimeout(5_000) {
                while (streaksRepository.streakStatsFlow.first().rosarioTotal != total) delay(1)
            }
        }
    }

    private suspend fun totalRosarios(): Int =
        withContext(Dispatchers.IO) { streaksRepository.streakStatsFlow.first().rosarioTotal }
}
