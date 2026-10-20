package com.example

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
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
 * "← Anterior" retrocede. Los selectores de misterio, oraciones y avance viven en la hoja
 * de ajustes, así que el test la abre con el engranaje antes de tocar cada opción.
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

    /** Abre la hoja de ajustes y espera a que su contenido esté compuesto. */
    private fun abrirAjustes() {
        waitForTag("rosario_ajustes")
        composeRule.onNodeWithTag("rosario_ajustes").performClick()
        waitForTag("rosario_ajustes_sheet")
    }

    /** Cierra la hoja con "Listo" y espera a que desaparezca antes de seguir rezando. */
    private fun cerrarAjustes() {
        composeRule.onNodeWithTag("rosario_ajustes_listo").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("rosario_ajustes_sheet").fetchSemanticsNodes().isEmpty()
        }
    }

    /** Deja activa una oración opcional sin depender del estado que haya quedado de otra corrida. */
    private fun asegurarOpcionActiva(tag: String) {
        val config = composeRule.onNodeWithTag(tag).fetchSemanticsNode().config
        val activa = SemanticsProperties.Selected in config &&
            config[SemanticsProperties.Selected]
        if (!activa) composeRule.onNodeWithTag(tag).performClick()
    }

    /** Normaliza el rezo: avance apagado, misterio automático y las tres oraciones activas. */
    private fun normalizarAjustes() {
        abrirAjustes()
        waitForTag("rosario_auto_apagado")
        composeRule.onNodeWithTag("rosario_auto_apagado").performClick()
        waitForTag("rosario_misterio_automatico")
        composeRule.onNodeWithTag("rosario_misterio_automatico").performClick()
        waitForTag("rosario_opcion_fatima")
        asegurarOpcionActiva("rosario_opcion_fatima")
        asegurarOpcionActiva("rosario_opcion_salve")
        asegurarOpcionActiva("rosario_opcion_oracion_final")
        cerrarAjustes()
    }

    @Test
    fun homeRosarioCard_opensTheRunner_andAdvancesWithTheBeads() {
        // Espera a que el Home esté listo y abre el rosario desde su tarjeta de racha.
        waitForTag("streak_card_rosario")
        composeRule.onNodeWithTag("streak_card_rosario").performClick()

        // El runner resuelve el progreso del día de forma asíncrona: espera los botones.
        waitForTag("rosario_next")

        // El avance automático pudo quedar activo de otra corrida: se apaga antes de normalizar
        // el paso, para que no avance mientras el test recorre el rosario.
        normalizarAjustes()

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
        composeRule.onAllNodesWithTag("rosario_hilo").assertCountEquals(0)

        // Octavo paso: primer misterio del día, con sus diez cuentas apagadas y el hilo tenue.
        repeat(7) { composeRule.onNodeWithTag("rosario_next").performClick() }
        composeRule.onNodeWithText("Paso 8 de 90").assertIsDisplayed()
        composeRule.onNodeWithText("Misterio 1 de 5 ·", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithTag("rosario_cuenta").assertCountEquals(10)
        composeRule.onAllNodesWithTag("rosario_hilo").assertCountEquals(1)
        composeRule.onAllNodes(hasTestTag("rosario_cuenta") and isSelected()).assertCountEquals(0)

        // Padrenuestro y primera avemaría: se enciende una sola cuenta.
        composeRule.onNodeWithTag("rosario_next").performClick()
        composeRule.onNodeWithTag("rosario_next").performClick()
        composeRule.onNodeWithText("1ª Avemaría").assertIsDisplayed()
        composeRule.onAllNodes(hasTestTag("rosario_cuenta") and isSelected()).assertCountEquals(1)

        // "← Anterior" vuelve al padrenuestro de la decena.
        composeRule.onNodeWithTag("rosario_prev").performClick()
        composeRule.onNodeWithText("Paso 9 de 90").assertIsDisplayed()

        // Hasta la décima avemaría: el hilo y las diez cuentas quedan completos.
        repeat(10) { composeRule.onNodeWithTag("rosario_next").performClick() }
        composeRule.onNodeWithText("10ª Avemaría").assertIsDisplayed()
        composeRule.onAllNodes(hasTestTag("rosario_cuenta") and isSelected()).assertCountEquals(10)

        // La decena siguiente reinicia el hilo y las cuentas.
        repeat(4) { composeRule.onNodeWithTag("rosario_next").performClick() }
        composeRule.onNodeWithText("Misterio 2 de 5 ·", substring = true).assertIsDisplayed()
        composeRule.onAllNodes(hasTestTag("rosario_cuenta") and isSelected()).assertCountEquals(0)

        // El selector manual fija otro misterio: vuelve al inicio con la lista nueva.
        abrirAjustes()
        composeRule.onNodeWithTag("rosario_misterio_dolorosos").performClick()
        composeRule.onNodeWithTag("rosario_misterio_dolorosos").assertIsSelected()
        cerrarAjustes()
        composeRule.onNodeWithText("Paso 1 de 90").assertIsDisplayed()

        repeat(7) { composeRule.onNodeWithTag("rosario_next").performClick() }
        composeRule.onNodeWithText("1.º Misterio Dolorosos").assertIsDisplayed()
        composeRule.onNodeWithText("Misterio 1 de 5 · Dolorosos").assertIsDisplayed()

        // Se vuelve a Automático para no dejar la preferencia fija en el dispositivo.
        abrirAjustes()
        composeRule.onNodeWithTag("rosario_misterio_automatico").performClick()
        composeRule.onNodeWithTag("rosario_misterio_automatico").assertIsSelected()
        cerrarAjustes()
        composeRule.onNodeWithText("Paso 1 de 90").assertIsDisplayed()

        // Apagar la Salve acorta la lista y reinicia el rezo; volver a encenderla restaura 90.
        abrirAjustes()
        composeRule.onNodeWithTag("rosario_opcion_salve").performClick()
        composeRule.onNodeWithTag("rosario_opcion_salve").assertIsNotSelected()
        cerrarAjustes()
        composeRule.onNodeWithText("Paso 1 de 89").assertIsDisplayed()

        abrirAjustes()
        composeRule.onNodeWithTag("rosario_opcion_salve").performClick()
        composeRule.onNodeWithTag("rosario_opcion_salve").assertIsSelected()
        cerrarAjustes()
        composeRule.onNodeWithText("Paso 1 de 90").assertIsDisplayed()

        // El avance automático en Rápido pasa solo al siguiente paso al terminar el tiempo de
        // rezo; se apaga enseguida para no dejarlo activo en el dispositivo.
        abrirAjustes()
        composeRule.onNodeWithTag("rosario_auto_rapido").performClick()
        cerrarAjustes()
        composeRule.waitUntil(30_000) {
            // Durante el avance la cabecera agrega " · Auto"; el texto exacto cambia después.
            composeRule.onAllNodesWithText("Paso 2 de 90", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        abrirAjustes()
        composeRule.onNodeWithTag("rosario_auto_apagado").performClick()
        composeRule.onNodeWithTag("rosario_auto_apagado").assertIsSelected()
        cerrarAjustes()
        composeRule.onNodeWithText("Paso 2 de 90").assertIsDisplayed()
    }

    @Test
    fun languageChange_midDecade_keepsTheStepAndIndex() {
        // Abre el rosario y normaliza el avance y el misterio para que el índice sea predecible.
        waitForTag("streak_card_rosario")
        composeRule.onNodeWithTag("streak_card_rosario").performClick()
        waitForTag("rosario_next")
        normalizarAjustes()

        // Fija Dolorosos para que los textos no dependan del día de la corrida.
        abrirAjustes()
        composeRule.onNodeWithTag("rosario_misterio_dolorosos").performClick()
        composeRule.onNodeWithTag("rosario_misterio_dolorosos").assertIsSelected()
        cerrarAjustes()

        // Deja el rezo al inicio y avanza hasta el primer misterio (paso 8).
        var vueltas = 0
        while (hayNodo("rosario_prev") && vueltas < 100) {
            composeRule.onNodeWithTag("rosario_prev").performClick()
            vueltas++
        }
        repeat(7) { composeRule.onNodeWithTag("rosario_next").performClick() }
        composeRule.onNodeWithText("Paso 8 de 90").assertIsDisplayed()
        composeRule.onNodeWithText("1.º Misterio Dolorosos").assertIsDisplayed()

        // Cambia a latín a mitad de la decena: mismo índice, mismos ids, textos nuevos.
        abrirAjustes()
        waitForTag("rosario_idioma_latin")
        composeRule.onNodeWithTag("rosario_idioma_latin").performClick()
        composeRule.onNodeWithTag("rosario_idioma_latin").assertIsSelected()
        cerrarAjustes()
        composeRule.onNodeWithText("Paso 8 de 90").assertIsDisplayed()
        composeRule.onNodeWithText("Mysterium I Dolorosum").assertIsDisplayed()
        composeRule.onNodeWithText("Mysterium 1 ex 5 · Dolorosa").assertIsDisplayed()

        // Vuelve a español: el paso sigue siendo el octavo y el texto regresa al original.
        abrirAjustes()
        composeRule.onNodeWithTag("rosario_idioma_espanol").performClick()
        composeRule.onNodeWithTag("rosario_idioma_espanol").assertIsSelected()
        cerrarAjustes()
        composeRule.onNodeWithText("Paso 8 de 90").assertIsDisplayed()
        composeRule.onNodeWithText("1.º Misterio Dolorosos").assertIsDisplayed()

        // Deja el misterio en Automático para no fijar la preferencia en el dispositivo.
        abrirAjustes()
        composeRule.onNodeWithTag("rosario_misterio_automatico").performClick()
        composeRule.onNodeWithTag("rosario_misterio_automatico").assertIsSelected()
        cerrarAjustes()
    }
}
