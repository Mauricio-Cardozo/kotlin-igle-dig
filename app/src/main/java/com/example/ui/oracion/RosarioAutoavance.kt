package com.example.ui.oracion

import kotlin.math.roundToInt

/**
 * Velocidad del avance automático del rosario. [APAGADO] deja el rezo en modo manual; los
 * valores 0–3 se guardan en DataStore para conservar la elección entre sesiones.
 */
enum class Autoavance(val valor: Int, val etiqueta: String, val factor: Double) {
    APAGADO(0, "Off", 1.0),
    LENTO(1, "Lento", 1.5),
    NORMAL(2, "Normal", 1.0),
    RAPIDO(3, "Rápido", 0.7);

    companion object {
        fun desdeValor(valor: Int): Autoavance =
            entries.firstOrNull { it.valor == valor } ?: APAGADO
    }
}

/** Ritmo de rezo pausado, en palabras por segundo, más una pausa fija entre pasos. */
private const val PALABRAS_POR_SEGUNDO = 1.8
private const val SEGUNDOS_DE_TRANSICION = 3.0

/**
 * Duración de un paso según la longitud de su texto, no una tabla fija: las letanías duran
 * más que un Gloria y cualquier oración nueva queda cubierta. Lento alarga un 50 % y Rápido
 * acorta un 30 %. No se copian los tiempos medidos de Rosarium (MIT, © 2024 Krixec), que
 * responden a otro idioma y a otro ritmo.
 */
fun duracionPasoSegundos(paso: RosarioStep, velocidad: Autoavance): Int {
    if (velocidad == Autoavance.APAGADO) return 0

    val palabras = paso.text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    val segundos = (SEGUNDOS_DE_TRANSICION + palabras / PALABRAS_POR_SEGUNDO) * velocidad.factor
    return segundos.roundToInt().coerceIn(5, 240)
}
