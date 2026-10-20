package com.example

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
 * La pantalla de licencias se alcanza desde el placeholder de Ajustes (hasta que la Fase 6
 * arme la sección INFORMACIÓN) y muestra el aviso MIT+Credo completo de Rosarium.
 */
@RunWith(AndroidJUnit4::class)
class LicenciasUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitForTag(tag: String, timeoutMillis: Long = 90_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun ajustesPlaceholder_opensLicencias_withTheRosariumLicense() {
        waitForTag("bottom_nav_bar")
        composeRule.onNodeWithTag("btn_ajustes").performClick()
        waitForTag("coming_soon_ajustes")

        composeRule.onNodeWithTag("ajustes_licencias").performClick()
        waitForTag("screen_licencias")
        composeRule.onNodeWithTag("licencias_texto").assertExists()
        composeRule.onNodeWithText("Copyright 2024, Krixec", substring = true).assertExists()
        composeRule.onNodeWithText("Credo in Deum,", substring = true).assertExists()

        // Cuando Ajustes ya salió de composición, el back del header vuelve a él.
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("coming_soon_ajustes").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag("btn_back").performClick()
        waitForTag("coming_soon_ajustes")
    }
}
