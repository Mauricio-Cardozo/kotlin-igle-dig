package com.example

import com.example.ui.ajustes.LicenciasTextos
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La licencia MIT+Credo exige que las copias incluyan el aviso de copyright, el permiso y el
 * Credo; estos tests protegen esas partes ante ediciones accidentales del texto de la pantalla.
 */
class LicenciasTextosTest {

    @Test
    fun licencia_incluyeCopyrightYPermisoDeKrixec() {
        assertTrue(LicenciasTextos.ROSARIUM.contains("Copyright 2024, Krixec"))
        assertTrue(LicenciasTextos.ROSARIUM.contains("Permission is hereby granted"))
        assertTrue(LicenciasTextos.ROSARIUM.contains("CREDO shall be included"))
    }

    @Test
    fun licencia_incluyeElCredoEnLatinCompleto() {
        assertTrue(LicenciasTextos.ROSARIUM.contains("Credo in Deum,"))
        assertTrue(LicenciasTextos.ROSARIUM.contains("Credo in Spiritum Sanctum,"))
        assertTrue(LicenciasTextos.ROSARIUM.contains("carnis resurrectionem,"))
        assertTrue(LicenciasTextos.ROSARIUM.contains("Amen."))
    }

    @Test
    fun licencia_noPierdeElDescargoDeResponsabilidad() {
        assertTrue(LicenciasTextos.ROSARIUM.contains("THE SOFTWARE IS PROVIDED"))
    }
}
