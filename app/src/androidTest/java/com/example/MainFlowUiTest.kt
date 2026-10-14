package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.db.IglesiaDatabaseHelper
import com.example.data.repository.LiturgiaRepository
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

/**
 * Fase 0.3 — Verificación end-to-end en dispositivo: la app arranca, la pestaña Calendario
 * muestra la grilla del mes y tocar un día con lectura navega al Evangelio del Día.
 */
@RunWith(AndroidJUnit4::class)
class MainFlowUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitForTag(tag: String, timeoutMillis: Long = 90_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun calendarioTab_showsGrid_andTappingDayWithReading_opensEvangelio() {
        // Espera a que la base se inicialice y aparezca la navegación inferior.
        waitForTag("bottom_nav_bar")

        // Abre la pestaña Calendario.
        composeRule.onNodeWithTag("tab_calendario").performClick()
        waitForTag("screen_calendario")
        composeRule.onNodeWithTag("calendario_mes_label").assertIsDisplayed()

        // Averigua un día con lectura del mes actual (según el reloj del dispositivo).
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbHelper = IglesiaDatabaseHelper(context)
        runBlocking { dbHelper.initializeDatabase() }
        val repo = LiturgiaRepository(dbHelper)
        val cal = Calendar.getInstance()
        val ano = cal.get(Calendar.YEAR)
        val mes = cal.get(Calendar.MONTH) + 1
        val lecturas = runBlocking { repo.getLecturasDelMes(ano, mes) }
        val fecha = lecturas.firstOrNull()?.fecha ?: error("el mes $mes/$ano no tiene lecturas en la base")

        // Toca el día con lectura -> debe navegar al Evangelio.
        waitForTag("calendario_dia_$fecha")
        composeRule.onNodeWithTag("calendario_dia_$fecha").performClick()
        waitForTag("screen_evangelio")
        composeRule.onNodeWithTag("screen_evangelio").assertIsDisplayed()

        // El contenido de las lecturas debe renderizarse.
        waitForTag("evangelio_lazy_column")
        composeRule.onNodeWithTag("evangelio_lazy_column").assertIsDisplayed()
    }
}
