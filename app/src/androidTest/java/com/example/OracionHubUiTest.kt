package com.example

import androidx.compose.ui.test.assertIsDisplayed
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
 * Flujo del hub de Oración: el tab lista las seis tarjetas de la RN, muestra las
 * pendientes con su placeholder y abre el rosario desde su tarjeta.
 */
@RunWith(AndroidJUnit4::class)
class OracionHubUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitForTag(tag: String, timeoutMillis: Long = 90_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun oracionTab_listsTheSixCards_andOpensThePendingOnes() {
        waitForTag("bottom_nav_bar")
        composeRule.onNodeWithTag("tab_oracion").performClick()
        waitForTag("oracion_card_rosario")

        // Encabezado y las seis tarjetas del hub, en el orden de la RN.
        composeRule.onNodeWithTag("header_title").assertIsDisplayed()
        composeRule.onNodeWithText("Santo Rosario").assertIsDisplayed()
        composeRule.onNodeWithText("Coronilla").assertIsDisplayed()
        composeRule.onNodeWithText("Favoritos").assertIsDisplayed()
        composeRule.onNodeWithText("Oraciones").assertIsDisplayed()
        composeRule.onNodeWithText("Jaculatorias").assertIsDisplayed()
        composeRule.onNodeWithText("Novenas").assertIsDisplayed()

        // La tarjeta pendiente abre su placeholder y el back vuelve al hub.
        composeRule.onNodeWithTag("oracion_card_coronilla").performClick()
        waitForTag("coming_soon_coronilla_de_la_misericordia")
        composeRule.onNodeWithTag("coming_soon_coronilla_de_la_misericordia").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_back").performClick()
        waitForTag("oracion_card_rosario")
        composeRule.onNodeWithTag("screen_oracion").assertIsDisplayed()
    }

    @Test
    fun oracionTab_opensTheRosarioFromTheHub() {
        waitForTag("bottom_nav_bar")
        composeRule.onNodeWithTag("tab_oracion").performClick()
        waitForTag("oracion_card_rosario")

        // Mismo runner que abre el Home desde la tarjeta de racha.
        composeRule.onNodeWithTag("oracion_card_rosario").performClick()
        waitForTag("screen_rosario")
        composeRule.onNodeWithTag("screen_rosario").assertIsDisplayed()
    }
}
