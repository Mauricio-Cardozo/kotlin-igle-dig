package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Church
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom navigation bar destinations.
 */
enum class BottomNavTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    LITURGIA("liturgia", "Liturgia", Icons.Filled.Church, Icons.Outlined.Church, "tab_liturgia"),
    BIBLIA("biblia_root", "Biblia", Icons.Filled.AutoStories, Icons.Outlined.AutoStories, "tab_biblia"),
    CALENDARIO("calendario", "Calendario", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "tab_calendario"),
    ORACION("oracion", "Oración", Icons.Filled.VolunteerActivism, Icons.Outlined.VolunteerActivism, "tab_oracion");
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
