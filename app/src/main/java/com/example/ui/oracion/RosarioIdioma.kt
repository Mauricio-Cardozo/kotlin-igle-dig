package com.example.ui.oracion

/**
 * Idioma del rezo del rosario. La interfaz de la app sigue siempre en español; solo cambian
 * las oraciones, los títulos de paso y los nombres de los misterios.
 *
 * La clave se guarda en DataStore. [desdeClave] cae en español ante un valor desconocido
 * (una preferencia vieja o un idioma que ya no exista), para no romper el arranque.
 */
enum class IdiomaRosario(val clave: String, val etiqueta: String) {
    ESPANOL("es", "Español"),
    LATIN("la", "Latín");

    companion object {
        fun desdeClave(clave: String?): IdiomaRosario =
            entries.firstOrNull { it.clave == clave } ?: ESPANOL
    }
}
