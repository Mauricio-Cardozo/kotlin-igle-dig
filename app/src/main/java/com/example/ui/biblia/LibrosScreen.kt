package com.example.ui.biblia

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.db.BibliaVersion
import com.example.data.db.LibroBiblia
import com.example.data.db.VersiculoBiblia
import com.example.ui.common.Buscador
import com.example.ui.common.EmptyState
import com.example.ui.common.ErrorState
import com.example.ui.common.LoadingState
import com.example.ui.common.ScreenHeader
import com.example.ui.common.pressable
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtle
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.Radii
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Nivel 1 del flujo bíblico: hero con imagen, buscador, filtros y las 9
 * secciones canónicas en grilla de 2 columnas, en paridad con la RN.
 */
@Composable
fun LibrosScreen(
    viewModel: BibliaViewModel,
    onLibroClick: (String) -> Unit,
    onSearchResultClick: (String, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val librosState by viewModel.librosState.collectAsStateWithLifecycle()
    val filtro by viewModel.filtroTestamento.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val busquedaState by viewModel.busquedaState.collectAsStateWithLifecycle()
    val version by viewModel.bibliaVersion.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .testTag("screen_libros")
    ) {
        when (val state = librosState) {
            is LibrosUiState.Loading -> LoadingState(message = "Cargando…")
            is LibrosUiState.Error -> ErrorState(
                message = state.message,
                onRetry = viewModel::loadLibros
            )
            is LibrosUiState.Success -> {
                if (searchQuery.isNotBlank()) {
                    BusquedaEnLaBiblia(
                        query = searchQuery,
                        state = busquedaState,
                        onQueryChange = viewModel::onSearchQueryChanged,
                        onClear = viewModel::clearSearch,
                        onResultClick = onSearchResultClick,
                        onLoadMore = viewModel::loadMoreResultados
                    )
                } else {
                    CatalogoDeLibros(
                        libros = state.libros,
                        filtro = filtro,
                        version = version,
                        query = searchQuery,
                        onQueryChange = viewModel::onSearchQueryChanged,
                        onClearSearch = viewModel::clearSearch,
                        onFiltroChange = viewModel::setFiltroTestamento,
                        onVersionChange = viewModel::setBibliaVersion,
                        onLibroClick = onLibroClick
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Nivel de búsqueda (pantalla dedicada como en la RN)
// ---------------------------------------------------------------------------

@Composable
private fun BusquedaEnLaBiblia(
    query: String,
    state: BusquedaUiState,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onResultClick: (String, Int, Int) -> Unit,
    onLoadMore: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Buscar en la Biblia",
            superLabel = "✝ IGLESIA DIGITAL",
            showBack = true,
            onBack = onClear
        )
        Buscador(
            value = query,
            onValueChange = onQueryChange,
            onClear = onClear,
            placeholder = "Buscá palabras…"
        )

        when (state) {
            // Esperando el debounce de 300 ms antes de disparar la búsqueda.
            is BusquedaUiState.Idle -> Spacer(modifier = Modifier.fillMaxSize())
            is BusquedaUiState.Searching -> LoadingState(message = "Cargando…")
            is BusquedaUiState.Error -> ErrorState(
                message = state.message,
                onRetry = { onQueryChange(query) }
            )
            is BusquedaUiState.Success -> {
                if (state.resultados.isEmpty()) {
                    EmptyState(
                        title = "Sin resultados",
                        subtitle = "No se encontraron versículos que coincidan con la búsqueda."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("search_results_lazy_column")
                    ) {
                        items(state.resultados, key = { it.id }) { versiculo ->
                            SearchResultRow(
                                versiculo = versiculo,
                                onClick = {
                                    onResultClick(
                                        versiculo.libro,
                                        versiculo.capitulo,
                                        versiculo.versiculo
                                    )
                                }
                            )
                        }
                        if (state.hayMas) {
                            item(key = "load_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.cargandoMas) {
                                        CircularProgressIndicator(
                                            color = GoldPrimary,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("load_more_progress")
                                        )
                                    } else {
                                        TextButton(
                                            onClick = onLoadMore,
                                            modifier = Modifier.testTag("load_more_button")
                                        ) {
                                            Text(
                                                text = "Ver más resultados",
                                                color = GoldLight,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(versiculo: VersiculoBiblia, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(pressedAlpha = 0.75f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("search_result_${versiculo.id}")
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Radii.sm))
                .background(GoldSubtle)
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = versiculo.referencia,
                color = GoldLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = versiculo.texto,
            color = TextPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Catálogo de libros (hero + buscador + filtros + 9 secciones)
// ---------------------------------------------------------------------------

@Composable
private fun CatalogoDeLibros(
    libros: List<LibroBiblia>,
    filtro: FiltroTestamento,
    version: BibliaVersion,
    query: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onFiltroChange: (FiltroTestamento) -> Unit,
    onVersionChange: (BibliaVersion) -> Unit,
    onLibroClick: (String) -> Unit
) {
    val antiguos = libros.count { it.testamento.equals("Antiguo", ignoreCase = true) }
    val nuevos = libros.count { it.testamento.equals("Nuevo", ignoreCase = true) }
    val grupos = when (filtro) {
        FiltroTestamento.TODOS -> GRUPOS_BIBLIA
        FiltroTestamento.ANTIGUO -> GRUPOS_BIBLIA.filter { it.testamento == "Antiguo" }
        FiltroTestamento.NUEVO -> GRUPOS_BIBLIA.filter { it.testamento == "Nuevo" }
    }

    // La RN remonta la lista al cambiar de filtro (key={filtro}); replicamos
    // el efecto volviendo al tope.
    val listState = rememberLazyListState()
    LaunchedEffect(filtro) { listState.scrollToItem(0) }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 24.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("books_lazy_column")
    ) {
        item(key = "hero") {
            BibliaHero(
                versionLabel = version.label,
                totalLibros = antiguos + nuevos,
                filtro = filtro
            )
        }
        item(key = "buscador") {
            Buscador(
                value = query,
                onValueChange = onQueryChange,
                onClear = onClearSearch,
                placeholder = "Buscá en la Biblia…"
            )
        }
        item(key = "filtro_testamento") {
            PillRow {
                BibliaPill(
                    label = "Antiguo ($antiguos)",
                    active = filtro == FiltroTestamento.ANTIGUO,
                    onClick = { onFiltroChange(FiltroTestamento.ANTIGUO) },
                    testTag = "filter_antiguo",
                    modifier = Modifier.weight(1f)
                )
                BibliaPill(
                    label = "Nuevo ($nuevos)",
                    active = filtro == FiltroTestamento.NUEVO,
                    onClick = { onFiltroChange(FiltroTestamento.NUEVO) },
                    testTag = "filter_nuevo",
                    modifier = Modifier.weight(1f)
                )
                BibliaPill(
                    label = "Todos (${antiguos + nuevos})",
                    active = filtro == FiltroTestamento.TODOS,
                    onClick = { onFiltroChange(FiltroTestamento.TODOS) },
                    testTag = "filter_todos",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item(key = "deut_note") {
            Text(
                text = "† deuterocanónico",
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 4.dp)
            )
        }
        item(key = "selector_version") {
            PillRow {
                BibliaVersion.entries.forEach { v ->
                    BibliaPill(
                        label = v.label,
                        active = version == v,
                        onClick = { onVersionChange(v) },
                        testTag = "version_${v.key}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        grupos.forEach { grupo ->
            item(key = "grupo_${grupo.nombre}") {
                GrupoCanonico(grupo = grupo, onLibroClick = onLibroClick)
            }
        }
    }
}

@Composable
private fun BibliaHero(
    versionLabel: String,
    totalLibros: Int,
    filtro: FiltroTestamento
) {
    val imageRes = if (filtro == FiltroTestamento.NUEVO) {
        R.drawable.biblia_nuevo
    } else {
        R.drawable.biblia_antiguo
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xA60D1B2A))
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✝ BIBLIA ${versionLabel.uppercase()}",
                color = GoldPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "La Sagrada Escritura",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "$totalLibros Libros",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun PillRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun BibliaPill(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(Radii.xxl)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (active) GoldPrimary else Color.Transparent)
            .border(1.dp, if (active) GoldPrimary else GoldSubtle, shape)
            .pressable(pressedAlpha = 0.7f, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) NavyBackground else GoldPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GrupoCanonico(grupo: GrupoBiblia, onLibroClick: (String) -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = grupo.nombre,
                color = GoldPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${grupo.libros.size} libros",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.alignByBaseline()
            )
        }
        grupo.libros.chunked(2).forEach { fila ->
            Row(modifier = Modifier.fillMaxWidth()) {
                fila.forEach { ref ->
                    LibroCard(
                        nombre = ref.libro,
                        onClick = { onLibroClick(ref.libro) },
                        deuterocanonico = ref.deuterocanonico,
                        modifier = Modifier
                            .weight(1f)
                            .padding(6.dp)
                    )
                }
                if (fila.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
