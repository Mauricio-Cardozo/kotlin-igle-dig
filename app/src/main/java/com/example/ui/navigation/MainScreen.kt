package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.AppContainer
import com.example.ui.biblia.BibliaViewModel
import com.example.ui.biblia.BibliaViewModelFactory
import com.example.ui.biblia.CapitulosScreen
import com.example.ui.biblia.LecturaScreen
import com.example.ui.biblia.LibrosScreen
import com.example.ui.common.ComingSoonScreen
import com.example.ui.liturgia.LiturgiaScreen
import com.example.ui.liturgia.LiturgiaViewModel
import com.example.ui.liturgia.LiturgiaViewModelFactory
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtle
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySeparator
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MainScreen(
    appContainer: AppContainer,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val liturgiaViewModel: LiturgiaViewModel = viewModel(
        factory = LiturgiaViewModelFactory(
            appContainer.liturgiaRepository,
            appContainer.streaksPreferencesRepository
        )
    )

    val bibliaViewModel: BibliaViewModel = viewModel(
        factory = BibliaViewModelFactory(
            appContainer.bibliaRepository,
            appContainer.userPreferencesRepository
        )
    )

    // Check which tab is active
    val isBibliaTab = currentRoute?.startsWith("biblia_") == true
    val isLiturgiaTab = currentRoute == null || currentRoute == BottomNavTab.LITURGIA.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = NavySurface,
                contentColor = TextPrimary,
                modifier = Modifier
                    .border(width = 1.dp, color = NavySeparator)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                BottomNavTab.entries.forEach { tab ->
                    val selected = when (tab) {
                        BottomNavTab.BIBLIA -> isBibliaTab
                        BottomNavTab.LITURGIA -> isLiturgiaTab
                        else -> currentRoute == tab.route
                    }

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (tab == BottomNavTab.BIBLIA) {
                                navController.navigate(BibliaDestinations.LIBROS) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                tint = if (selected) NavyBackground else TextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                color = if (selected) GoldLight else TextSecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavyBackground,
                            unselectedIconColor = TextSecondary,
                            selectedTextColor = GoldLight,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = GoldPrimary
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        },
        containerColor = NavyBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavTab.LITURGIA.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Liturgia Tab (Home)
            composable(BottomNavTab.LITURGIA.route) {
                LiturgiaScreen(
                    viewModel = liturgiaViewModel,
                    onNavigateToEvangelio = { navController.navigate(LiturgiaDestinations.EVANGELIO) },
                    onNavigateToRosario = { navController.navigate(LiturgiaDestinations.ROSARIO_GUIA) },
                    onNavigateToCoronilla = { navController.navigate(LiturgiaDestinations.ROSARIO_CORONILLA) },
                    onNavigateToSanto = { id -> navController.navigate(LiturgiaDestinations.santoDetalleRoute(id)) },
                    onNavigateToMisalPropio = { navController.navigate(LiturgiaDestinations.MISAL_PROPIO) },
                    onNavigateToAjustes = { navController.navigate(LiturgiaDestinations.AJUSTES) }
                )
            }

            // Destinations originating from Liturgia
            composable(LiturgiaDestinations.EVANGELIO) {
                ComingSoonScreen(
                    title = "Evangelio del Día",
                    icon = Icons.Default.Church,
                    description = "El texto completo de las lecturas y el Evangelio del día estará disponible en la próxima pantalla de lectura litúrgica.",
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(LiturgiaDestinations.ROSARIO_GUIA) {
                ComingSoonScreen(
                    title = "Santo Rosario",
                    icon = Icons.Default.VolunteerActivism,
                    description = "La guía interactiva paso a paso del Santo Rosario con seguimiento de misterios y cuentas se integrará próximamente.",
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(LiturgiaDestinations.ROSARIO_CORONILLA) {
                ComingSoonScreen(
                    title = "Coronilla de la Misericordia",
                    icon = Icons.Default.VolunteerActivism,
                    description = "La guía interactiva de la Coronilla de la Divina Misericordia se integrará próximamente.",
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = LiturgiaDestinations.SANTO_DETALLE,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: 0L
                ComingSoonScreen(
                    title = "Santo del Día",
                    icon = Icons.Default.Church,
                    description = "La biografía detallada y oraciones para el santo (ID $id) se integrarán en la próxima fase.",
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(LiturgiaDestinations.MISAL_PROPIO) {
                ComingSoonScreen(
                    title = "Propio del Tiempo",
                    icon = Icons.Default.Church,
                    description = "El Misal Romano completo con las oraciones del Propio del Tiempo se integrará en la fase de Liturgia.",
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(LiturgiaDestinations.AJUSTES) {
                ComingSoonScreen(
                    title = "Ajustes",
                    icon = Icons.Default.Settings,
                    description = "Configuración de notificaciones, recordatorios, tamaño de letra y respaldo de datos.",
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Biblia Tab (3-level flow)
            // Level 1: Books List
            composable(BibliaDestinations.LIBROS) {
                LibrosScreen(
                    viewModel = bibliaViewModel,
                    onLibroClick = { libro ->
                        navController.navigate(BibliaDestinations.capitulosRoute(libro))
                    },
                    onSearchResultClick = { libro, capitulo, versiculo ->
                        navController.navigate(BibliaDestinations.lecturaRoute(libro, capitulo, versiculo))
                    }
                )
            }

            // Level 2: Chapters Grid
            composable(
                route = BibliaDestinations.CAPITULOS,
                arguments = listOf(
                    navArgument("libro") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val rawLibro = backStackEntry.arguments?.getString("libro").orEmpty()
                val libro = android.net.Uri.decode(rawLibro)
                CapitulosScreen(
                    libro = libro,
                    viewModel = bibliaViewModel,
                    onCapituloClick = { cap ->
                        navController.navigate(BibliaDestinations.lecturaRoute(libro, cap))
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            // Level 3: Verses Reading
            composable(
                route = BibliaDestinations.LECTURA,
                arguments = listOf(
                    navArgument("libro") { type = NavType.StringType },
                    navArgument("capitulo") { type = NavType.IntType },
                    navArgument("targetVersiculo") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val rawLibro = backStackEntry.arguments?.getString("libro").orEmpty()
                val libro = android.net.Uri.decode(rawLibro)
                val capitulo = backStackEntry.arguments?.getInt("capitulo") ?: 1
                val targetVersiculoParam = backStackEntry.arguments?.getInt("targetVersiculo")
                val targetVersiculo = if (targetVersiculoParam != null && targetVersiculoParam > 0) targetVersiculoParam else null

                LecturaScreen(
                    libro = libro,
                    capitulo = capitulo,
                    targetVersiculo = targetVersiculo,
                    viewModel = bibliaViewModel,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            // Calendario Tab
            composable(BottomNavTab.CALENDARIO.route) {
                ComingSoonScreen(
                    title = "Calendario",
                    icon = Icons.Default.CalendarMonth,
                    description = "El calendario litúrgico con las fiestas de los santos, solemnidades y tiempos litúrgicos estará disponible próximamente."
                )
            }

            // Oración Tab
            composable(BottomNavTab.ORACION.route) {
                ComingSoonScreen(
                    title = "Oración",
                    icon = Icons.Default.VolunteerActivism,
                    description = "El Santo Rosario, las novenas tradicionales y oraciones de los santos se integrarán en las próximas fases."
                )
            }
        }
    }
}
