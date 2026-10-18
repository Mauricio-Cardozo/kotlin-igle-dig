package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
import com.example.ui.common.pressable
import com.example.ui.liturgia.CalendarioScreen
import com.example.ui.liturgia.EvangelioScreen
import com.example.ui.liturgia.LiturgiaScreen
import com.example.ui.liturgia.LiturgiaViewModel
import com.example.ui.liturgia.LiturgiaViewModelFactory
import com.example.ui.liturgia.SantoDetalleScreen
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtle
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.TextSecondary

/**
 * Estado del tab bar flotante: replica el comportamiento de la RN, que lo oculta
 * al avanzar más de 50 px en el contenido y lo vuelve a mostrar al subir (o al
 * volver arriba).
 */
private class TabBarScrollState {
    var hidden by mutableStateOf(false)
        private set

    private var scrollY = 0f
    private var downAccum = 0f
    private var upAccum = 0f

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val dy = -available.y // > 0 = avanzando hacia abajo en el contenido
            if (dy != 0f) {
                scrollY = (scrollY + dy).coerceAtLeast(0f)
                if (dy > 0f) {
                    upAccum = 0f
                    downAccum += dy
                    if (!hidden && scrollY > HIDE_THRESHOLD && downAccum > 3f) {
                        hidden = true
                    }
                } else {
                    downAccum = 0f
                    upAccum += -dy
                    if (hidden && (upAccum > 3f || scrollY <= HIDE_THRESHOLD)) {
                        hidden = false
                    }
                }
            }
            return Offset.Zero
        }
    }

    fun reset() {
        hidden = false
        scrollY = 0f
        downAccum = 0f
        upAccum = 0f
    }

    private companion object {
        const val HIDE_THRESHOLD = 50f
    }
}

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

    // La barra solo se muestra en las pantallas de tabs (como en la RN, donde
    // Evangelio/Santo/Rosario/Ajustes quedan fuera del navegador de tabs).
    val showTabBar = isLiturgiaTab || isBibliaTab ||
        currentRoute == BottomNavTab.CALENDARIO.route || currentRoute == BottomNavTab.ORACION.route

    val tabBarScrollState = remember { TabBarScrollState() }
    LaunchedEffect(currentRoute) { tabBarScrollState.reset() }

    Scaffold(
        containerColor = NavyBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Compensa el modo inmersivo (barras del sistema ocultas):
                // el header de la RN también arranca a 12 dp del borde.
                .padding(top = 12.dp)
                .nestedScroll(tabBarScrollState.nestedScrollConnection)
        ) {
            NavHost(
                navController = navController,
                startDestination = BottomNavTab.LITURGIA.route,
                modifier = Modifier
                    .fillMaxSize()
                    // Deja aire para que la píldora flotante no tape el final
                    // del contenido (la RN agrega padding por pantalla).
                    .padding(bottom = if (showTabBar) 64.dp else 0.dp)
            ) {
                // Liturgia Tab (Home)
                composable(BottomNavTab.LITURGIA.route) {
                    LiturgiaScreen(
                        viewModel = liturgiaViewModel,
                        onNavigateToEvangelio = { navController.navigate(LiturgiaDestinations.evangelioRoute()) },
                        onNavigateToRosario = { navController.navigate(LiturgiaDestinations.ROSARIO_GUIA) },
                        onNavigateToCoronilla = { navController.navigate(LiturgiaDestinations.ROSARIO_CORONILLA) },
                        onNavigateToSanto = { id -> navController.navigate(LiturgiaDestinations.santoDetalleRoute(id)) },
                        onNavigateToAjustes = { navController.navigate(LiturgiaDestinations.AJUSTES) }
                    )
                }

                // Destinations originating from Liturgia
                composable(
                    route = LiturgiaDestinations.EVANGELIO,
                    arguments = listOf(
                        navArgument("fecha") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val fecha = backStackEntry.arguments?.getString("fecha")
                    EvangelioScreen(
                        viewModel = liturgiaViewModel,
                        fecha = fecha,
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
                    SantoDetalleScreen(
                        viewModel = liturgiaViewModel,
                        santoId = id,
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
                    CalendarioScreen(
                        viewModel = liturgiaViewModel,
                        onDiaClick = { fecha ->
                            navController.navigate(LiturgiaDestinations.evangelioRoute(fecha))
                        },
                        onSantoClick = { id ->
                            navController.navigate(LiturgiaDestinations.santoDetalleRoute(id))
                        }
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

            AnimatedVisibility(
                visible = showTabBar && !tabBarScrollState.hidden,
                enter = slideInVertically(animationSpec = tween(durationMillis = 200)) { it },
                exit = slideOutVertically(animationSpec = tween(durationMillis = 200)) { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                FloatingTabBar(
                    currentRoute = currentRoute,
                    isBibliaTab = isBibliaTab,
                    isLiturgiaTab = isLiturgiaTab,
                    onTabClick = { tab ->
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
                    }
                )
            }
        }
    }
}

/**
 * Píldora flotante equivalente al `floating-tab-bar` de la RN: radio 24, fondo
 * navy al 94 %, borde dorado, íconos de 22 dp sin etiquetas y punto de 4 dp
 * bajo el tab activo.
 */
@Composable
private fun FloatingTabBar(
    currentRoute: String?,
    isBibliaTab: Boolean,
    isLiturgiaTab: Boolean,
    onTabClick: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xF00D1B2A)) // rgba(13,27,42,0.94) de la RN
            .border(1.dp, GoldSubtle, RoundedCornerShape(24.dp))
            .padding(vertical = 8.dp)
            .testTag("bottom_nav_bar")
    ) {
        BottomNavTab.entries.forEach { tab ->
            val selected = when (tab) {
                BottomNavTab.BIBLIA -> isBibliaTab
                BottomNavTab.LITURGIA -> isLiturgiaTab
                else -> currentRoute == tab.route
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .weight(1f)
                    .pressable(pressedAlpha = 0.7f) { onTabClick(tab) }
                    .padding(vertical = 4.dp)
                    .testTag(tab.testTag)
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.title,
                    tint = if (selected) GoldPrimary else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (selected) GoldPrimary else Color.Transparent)
                )
            }
        }
    }
}
