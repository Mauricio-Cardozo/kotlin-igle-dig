package com.example.ui.oracion

/** Cuentas de una decena del rosario. */
const val CUENTAS_POR_DECENA = 10

/**
 * Fracción del hilo, entre el centro de la primera cuenta y el de la última, que cubren las
 * [completadas] avemarías. La primera cuenta todavía no une nada; con las diez el hilo está
 * completo. Los valores fuera de la decena se acotan para que el dibujo nunca se salga.
 */
fun fraccionHiloDecena(completadas: Int): Float {
    val cuentas = completadas.coerceIn(0, CUENTAS_POR_DECENA)
    return (cuentas - 1).coerceAtLeast(0) / (CUENTAS_POR_DECENA - 1).toFloat()
}
