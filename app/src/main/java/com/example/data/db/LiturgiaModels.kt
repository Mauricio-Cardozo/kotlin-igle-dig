package com.example.data.db

data class Lectura(
    val id: Long,
    val fecha: String,
    val url: String?,
    val tituloMisa: String?,
    val primeraLecturaRef: String?,
    val primeraLectura: String?,
    val salmo: String?,
    val aleluia: String?,
    val evangelioRef: String?,
    val evangelio: String?,
    val comentarioPapal: String?,
    val segundaLecturaRef: String? = null,
    val segundaLectura: String? = null
)

data class Santo(
    val id: Long,
    val mes: Int,
    val dia: Int,
    val nombre: String,
    val titulo: String?,
    val biografia: String
)

data class MisalPropioEntry(
    val id: Long,
    val temporada: String,
    val temporadaLabel: String?,
    val dia: String,
    val colecta: String?,
    val oracionOfrendas: String?,
    val postcomunion: String?,
    val prefacio: String?,
    val antifonaEntrada: String?,
    val antifonaComunion: String?
)

data class TemporadaInfo(
    val temporada: String,
    val temporadaLabel: String,
    val count: Int
)

data class NearestSeasonResult(
    val season: String,
    val week: Int,
    val isSunday: Boolean
)

/**
 * Entry from the `misal_santos` table (Proper of Saints / Misal del día del santo).
 */
data class MisalSantosEntry(
    val id: Long,
    val mes: Int,
    val dia: Int,
    val nombre: String,
    val titulo: String?,
    val rango: String?,
    val antifonaEntrada: String?,
    val colecta: String?,
    val oracionOfrendas: String?,
    val prefacio: String?,
    val antifonaComunion: String?,
    val postcomunion: String?
)

/**
 * Lightweight calendar entry for a day that has readings in the `lecturas` table.
 */
data class DiaLectura(
    val fecha: String,
    val tituloMisa: String?,
    val tieneEvangelio: Boolean
)
