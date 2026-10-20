package com.example.ui.oracion

import java.util.Locale

/**
 * Serie de misterios que corresponde rezar. A diferencia de [MisterioRosario] —la selección
 * manual, que incluye "Automático"— esta es la serie ya resuelta para la fecha.
 */
enum class SerieMisterios { GOZOSOS, LUMINOSOS, DOLOROSOS, GLORIOSOS }

/**
 * Nombre de una serie de misterios en el idioma del rezo. [titulo] se usa en el título del
 * paso ("1.º Misterio Gozosos" / "Mysterium I Gaudiosum") y [colectivo] en el subtítulo
 * ("Misterio 1 de 5 · Gozosos" / "Mysterium 1 ex 5 · Gaudiosa").
 */
data class NombreSerie(val titulo: String, val colectivo: String)

/**
 * Todos los textos variables del rezo del rosario, separados del generador de pasos para
 * poder ofrecer el rezo en varios idiomas. Los ids y el orden de los pasos no dependen del
 * idioma: cambiar de idioma no reinicia el rezo.
 *
 * Las plantillas se aplican con [String.format] y [Locale.ROOT]:
 * - [plantillaMisterio]: ordinal y nombre de la serie en su forma de título.
 * - [plantillaSubtituloMisterio]: número del misterio (1–5) y nombre colectivo.
 * - [plantillaAvemaria]: número de la avemaría (1–10).
 */
data class TextosRosario(
    // Títulos de los pasos
    val tituloSenal: String,
    val tituloContricion: String,
    val tituloPadrenuestro: String,
    val tituloGloria: String,
    val tituloOhJesus: String,
    val tituloMariaMadre: String,
    val tituloLetanias: String,
    val tituloOracionFinal: String,
    val tituloSalve: String,
    val tituloJaculatoria: String,
    val tituloAvemariaFinal: String,
    val tituloCompletado: String,

    // Plantillas de los pasos que se numeran
    val ordinales: List<String>,
    val plantillaMisterio: String,
    val plantillaSubtituloMisterio: String,
    val plantillaAvemaria: String,
    val intencionesDelSantoPadre: String,

    // Series de misterios y sus cinco decenas
    val nombresSeries: Map<SerieMisterios, NombreSerie>,
    val misterios: Map<SerieMisterios, List<String>>,

    // Oraciones
    val senal: String,
    val contricion: String,
    val padrenuestro: String,
    val avemaria: String,
    val gloria: String,
    val ohJesus: String,
    val mariaMadre: String,
    val letanias: String,
    val oracionFinal: String,
    val padrenuestroPapa: String,
    val avemariaFinal: String,
    val gloriaFinal: String,
    val salve: String,
    val jaculatoria: String,
    val completado: String
) {
    init {
        require(ordinales.size == 5) { "El rosario necesita cinco ordinales de misterio" }
        require(nombresSeries.keys.containsAll(SerieMisterios.entries)) {
            "Faltan nombres para alguna serie de misterios"
        }
        require(misterios.keys.containsAll(SerieMisterios.entries)) {
            "Faltan misterios para alguna serie"
        }
        require(misterios.values.all { it.size == 5 }) {
            "Cada serie de misterios debe tener cinco decenas"
        }
    }

    fun nombreSerie(serie: SerieMisterios): NombreSerie = nombresSeries.getValue(serie)

    fun misteriosDe(serie: SerieMisterios): List<String> = misterios.getValue(serie)

    /** Título del paso del misterio, p. ej. "1.º Misterio Gozosos". */
    fun tituloMisterio(indice: Int, serie: SerieMisterios): String = String.format(
        Locale.ROOT,
        plantillaMisterio,
        ordinales[indice],
        nombreSerie(serie).titulo
    )

    /** Subtítulo bajo las cuentas, p. ej. "Misterio 1 de 5 · Gozosos". */
    fun subtituloMisterio(indice: Int, serie: SerieMisterios): String = String.format(
        Locale.ROOT,
        plantillaSubtituloMisterio,
        indice + 1,
        nombreSerie(serie).colectivo
    )

    /** Título de una avemaría numerada, p. ej. "1ª Avemaría". */
    fun tituloAvemaria(numero: Int): String =
        String.format(Locale.ROOT, plantillaAvemaria, numero)
}
