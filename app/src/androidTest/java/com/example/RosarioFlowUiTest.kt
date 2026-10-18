package com.example

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Flujo end-to-end del rezo en dispositivo: el Home abre el rosario desde la tarjeta de
 * racha, "Amén · Siguiente" avanza, cada avemaría enciende una cuenta de la decena y
 * "← Anterior" retrocede.
 */
@RunWith(AndroidJUnit4::class)
class RosarioFlowUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitForTag(tag: String, timeoutMillis: Long = 90_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun hayNodo(tag: String): Boolean =
        composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun homeRosarioCard_opensTheRunner_andAdvancesWithTheBeads() {
        // Espera a que el Home esté listo y abre el rosario desde su tarjeta de racha.
        waitForTag("streak_card_rosario")
        composeRule.onNodeWithTag("streak_card_rosario").performClick()

        // El runner resuelve el progreso del día de forma asíncrona: espera los botones.
        waitForTag("rosario_next")

        // Deja el rezo al inicio (una corrida anterior del test pudo quedar a medias).
        var vueltas = 0
        while (hayNodo("rosario_prev") && vueltas < 100) {
            composeRule.onNodeWithTag("rosario_prev").performClick()
            vueltas++
        }

        // Paso 1: señal de la cruz, sin cuentas todavía.
        composeRule.onNodeWithText("Señal de la Cruz").assertIsDisplayed()
        composeRule.onNodeWithText("Paso 1 de 90").assertIsDisplayed()
        composeRule.onAllNodesWithTag("rosario_cuenta").assertCountEquals(0)

        // Octavo paso: primer misterio del día, con sus diez cuentas apagadas.
        repeat(7) { composeRule.onNodeWithTag("rosario_next").performClick() }
        composeRule.onNodeWithText("Paso 8 de 90").assertIsDisplayed()
        composeRule.onNodeWithText("Misterio 1 de 5 ·", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithTag("rosario_cuenta").assertCountEquals(10)
        composeRule.onAllNodes(hasTestTag("rosario_cuenta") and isSelected()).assertCountEquals(0)

        // Padrenuestro y primera avemaría: se enciende una sola cuenta.
        composeRule.onNodeWithTag("rosario_next").performClick()
        composeRule.onNodeWithTag("rosario_next").performClick()
        composeRule.onNodeWithText("1ª Avemaría").assertIsDisplayed()
        composeRule.onAllNodes(hasTestTag("rosario_cuenta") and isSelected()).assertCountEquals(1)

        // "← Anterior" vuelve al padrenuestro de la decena.
        composeRule.onNodeWithTag("rosario_prev").performClick()
        composeRule.onNodeWithText("Paso 9 de 90").assertIsDisplayed()
    }
}
