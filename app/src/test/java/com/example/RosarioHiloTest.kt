package com.example

import com.example.ui.oracion.fraccionHiloDecena
import org.junit.Assert.assertEquals
import org.junit.Test

class RosarioHiloTest {

    @Test
    fun fraccion_sinCuentasNoDibujaHilo() {
        assertEquals(0f, fraccionHiloDecena(0), 0.0001f)
    }

    @Test
    fun fraccion_unaCuentaTodaviaNoUneNada() {
        assertEquals(0f, fraccionHiloDecena(1), 0.0001f)
    }

    @Test
    fun fraccion_avanzaDeCentroACentro() {
        assertEquals(1f / 9f, fraccionHiloDecena(2), 0.0001f)
        assertEquals(5f / 9f, fraccionHiloDecena(6), 0.0001f)
        assertEquals(1f, fraccionHiloDecena(10), 0.0001f)
    }

    @Test
    fun fraccion_seAcotaFueraDeLaDecena() {
        assertEquals(0f, fraccionHiloDecena(-3), 0.0001f)
        assertEquals(1f, fraccionHiloDecena(12), 0.0001f)
    }
}
