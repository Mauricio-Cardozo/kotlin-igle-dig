package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.IglesiaDatabaseHelper
import com.example.data.repository.LiturgiaRepository
import com.example.data.user.StreaksPreferencesRepository
import com.example.ui.liturgia.LiturgiaUiState
import com.example.ui.liturgia.LiturgiaViewModel
import com.example.util.FixedDateProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiturgiaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var dbHelper: IglesiaDatabaseHelper
    private lateinit var liturgiaRepository: LiturgiaRepository
    private lateinit var streaksRepository: StreaksPreferencesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
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
        liturgiaRepository = LiturgiaRepository(dbHelper)
        streaksRepository = StreaksPreferencesRepository(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun LiturgiaViewModel.awaitLoaded(): LiturgiaUiState =
        uiState.first { !it.isLoading }

    @Test
    fun viewModel_loadsDomingoOctubre11() = runTest(testDispatcher) {
        val fixedDate = LocalDate.of(2026, 10, 11)
        val fixedTime = LocalTime.of(9, 30) // 9 AM -> "Bendecido día"
        val dateProvider = FixedDateProvider(fixedDate, fixedTime)

        val viewModel = LiturgiaViewModel(liturgiaRepository, streaksRepository, dateProvider)
        val state = viewModel.awaitLoaded()
        assertEquals("Domingo, 11 de Octubre", state.fechaLarga)
        assertEquals("Bendecido día", state.saludo)
        assertEquals("XXVIII Domingo ordinario", state.misaTitle)
        assertEquals("ordinario", state.season)

        assertNotNull(state.seasonInfo)
        assertEquals("Tiempo Ordinario", state.seasonInfo?.label)
        assertEquals("Verde", state.seasonInfo?.colorName)
        assertEquals("🌿", state.seasonInfo?.emoji)

        assertNotNull(state.gospelQuote)
        assertTrue(state.gospelQuote!!.startsWith("Mateo 22, 1-10"))
        assertEquals("Lectura del santo evangelio según san Mateo", state.gospelRef)

        assertNotNull(state.propio)
        assertEquals(284L, state.propio?.id)
    }

    @Test
    fun viewModel_headerFieldsReadyBeforeLoad() = runTest(testDispatcher) {
        val dateProvider = FixedDateProvider(LocalDate.of(2026, 10, 11), LocalTime.of(9, 30))
        val viewModel = LiturgiaViewModel(liturgiaRepository, streaksRepository, dateProvider)

        // Sin esperar la carga asíncrona: fecha y saludo ya están disponibles
        // (el Home pinta el encabezado al instante, como la RN).
        val initial = viewModel.uiState.value
        assertTrue(initial.isLoading)
        assertEquals("Domingo, 11 de Octubre", initial.fechaLarga)
        assertEquals("Bendecido día", initial.saludo)
    }

    @Test
    fun viewModel_greetingChangesAccordingToHour() = runTest(testDispatcher) {
        val fixedDate = LocalDate.of(2026, 10, 11)

        // 3 AM -> "Bendecida noche"
        val vmNight = LiturgiaViewModel(liturgiaRepository, streaksRepository, FixedDateProvider(fixedDate, LocalTime.of(3, 0)))
        vmNight.awaitLoaded()
        assertEquals("Bendecida noche", vmNight.uiState.value.saludo)

        // 14:00 (2 PM) -> "Bendecida tarde"
        val vmAfternoon = LiturgiaViewModel(liturgiaRepository, streaksRepository, FixedDateProvider(fixedDate, LocalTime.of(14, 0)))
        vmAfternoon.awaitLoaded()
        assertEquals("Bendecida tarde", vmAfternoon.uiState.value.saludo)

        // 22:00 (10 PM) -> "Bendecida noche"
        val vmLate = LiturgiaViewModel(liturgiaRepository, streaksRepository, FixedDateProvider(fixedDate, LocalTime.of(22, 0)))
        vmLate.awaitLoaded()
        assertEquals("Bendecida noche", vmLate.uiState.value.saludo)
    }

    @Test
    fun viewModel_handlesDateWithoutReadingsGracefully() = runTest(testDispatcher) {
        val fixedDate = LocalDate.of(2025, 12, 31) // outside DB range (2026-01-01..2027-01-02)
        val vm = LiturgiaViewModel(liturgiaRepository, streaksRepository, FixedDateProvider(fixedDate))
        val state = vm.awaitLoaded()

        assertNull(state.misaTitle)
        assertNull(state.gospelQuote)
        assertNull(state.gospelRef)
        assertNull(state.seasonInfo)
        assertNull(state.propio)
    }
}
