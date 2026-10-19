package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom navigation bar destinations (tab bar flotante, estilo RN).
 */
enum class BottomNavTab(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    LITURGIA("liturgia", "Liturgia", Icons.Filled.Home, "tab_liturgia"),
    BIBLIA("biblia_root", "Biblia", Icons.AutoMirrored.Filled.MenuBook, "tab_biblia"),
    CALENDARIO("calendario", "Calendario", Icons.Filled.CalendarMonth, "tab_calendario"),
    ORACION("oracion", "Oración", Icons.Filled.VolunteerActivism, "tab_oracion");
}

/**
 * Routes inside the Biblia sub-flow.
 */
object BibliaDestinations {
    const val LIBROS = "biblia_libros"
    const val CAPITULOS = "biblia_capitulos/{libro}"
    const val LECTURA = "biblia_lectura/{libro}/{capitulo}?targetVersiculo={targetVersiculo}"

    fun capitulosRoute(libro: String): String {
        val encoded = android.net.Uri.encode(libro)
        return "biblia_capitulos/$encoded"
    }

    fun lecturaRoute(libro: String, capitulo: Int, targetVersiculo: Int? = null): String {
        val encoded = android.net.Uri.encode(libro)
        return if (targetVersiculo != null) {
            "biblia_lectura/$encoded/$capitulo?targetVersiculo=$targetVersiculo"
        } else {
            "biblia_lectura/$encoded/$capitulo"
        }
    }
}

/**
 * Routes for destinations originating from the Home/Liturgia screen.
 */
object LiturgiaDestinations {
    const val EVANGELIO = "evangelio?fecha={fecha}"
    const val ROSARIO_GUIA = "rosario/guia"
    const val ROSARIO_CORONILLA = "rosario/coronilla"
    const val SANTO_DETALLE = "santo/{id}"
    const val MISAL_PROPIO = "misal/propio"
    const val AJUSTES = "ajustes"

    fun santoDetalleRoute(id: Long): String = "santo/$id"

    /**
     * Ruta del Evangelio del Día. Sin [fecha] muestra el día de hoy; con [fecha] (ISO YYYY-MM-DD)
     * muestra las lecturas de esa fecha (por ejemplo, al tocar un día del calendario).
     */
    fun evangelioRoute(fecha: String? = null): String =
        if (fecha.isNullOrBlank()) "evangelio" else "evangelio?fecha=$fecha"
}

/**
 * Routes for the destinations of the Oración hub (same shape as the routes of
 * `app/(tabs)/oracion.tsx` in the RN app).
 */
object OracionDestinations {
    const val FAVORITOS = "favoritos"
    const val ORACIONES = "oraciones"
    const val JACULATORIAS = "oraciones/jaculatorias"
    const val NOVENAS = "oraciones/novena"
}
