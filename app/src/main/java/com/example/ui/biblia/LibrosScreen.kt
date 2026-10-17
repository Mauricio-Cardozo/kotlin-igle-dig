package com.example.ui.biblia

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.BibliaVersion
import com.example.data.db.LibroBiblia
import com.example.data.db.VersiculoBiblia
import com.example.ui.common.EmptyState
import com.example.ui.common.ErrorState
import com.example.ui.common.LoadingState
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtle
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySeparator
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrosScreen(
    viewModel: BibliaViewModel,
    onLibroClick: (String) -> Unit,
    onSearchResultClick: (String, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val librosState by viewModel.librosState.collectAsStateWithLifecycle()
    val filtroTestamento by viewModel.filtroTestamento.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val busquedaState by viewModel.busquedaState.collectAsStateWithLifecycle()
    val bibliaVersion by viewModel.bibliaVersion.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .testTag("screen_libros")
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Santa Biblia",
                style = MaterialTheme.typography.headlineLarge,
                color = GoldLight,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = bibliaVersion.label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bible Version Selector (Pueblo de Dios / Navarra)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("biblia_version_selector")
            ) {
                BibliaVersion.entries.forEach { version ->
                    FiltroChipItem(
                        label = version.label,
                        selected = bibliaVersion == version,
                        onClick = { viewModel.setBibliaVersion(version) },
                        testTag = "version_${version.key}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = {
                    Text(
                        "Buscar en toda la Biblia (ej. amor prójimo)",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = GoldPrimary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = viewModel::clearSearch,
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar búsqueda",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NavySurface,
                    unfocusedContainerColor = NavySurface,
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = NavySeparator,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = GoldPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_field")
            )

            // Testament Filter Chips (only when not searching)
            if (searchQuery.isBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FiltroChipItem(
                        label = "Todos",
                        selected = filtroTestamento == FiltroTestamento.TODOS,
                        onClick = { viewModel.setFiltroTestamento(FiltroTestamento.TODOS) },
                        testTag = "filter_todos"
                    )
                    FiltroChipItem(
                        label = "Antiguo Testamento",
                        selected = filtroTestamento == FiltroTestamento.ANTIGUO,
                        onClick = { viewModel.setFiltroTestamento(FiltroTestamento.ANTIGUO) },
                        testTag = "filter_antiguo"
                    )
                    FiltroChipItem(
                        label = "Nuevo Testamento",
                        selected = filtroTestamento == FiltroTestamento.NUEVO,
                        onClick = { viewModel.setFiltroTestamento(FiltroTestamento.NUEVO) },
                        testTag = "filter_nuevo"
                    )
                }
            }
        }

        // Content Area: Search Results or Books List
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (searchQuery.isNotBlank()) {
                // Show Search Results
                SearchResultsView(
                    state = busquedaState,
                    onResultClick = onSearchResultClick,
                    onLoadMore = viewModel::loadMoreResultados
                )
            } else {
                // Show Books List
                BooksListView(
                    state = librosState,
                    filtro = filtroTestamento,
                    onLibroClick = onLibroClick,
                    onRetry = viewModel::loadLibros
                )
            }
        }
    }
}

@Composable
private fun FiltroChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) NavyBackground else TextPrimary,
                fontSize = 12.sp
            )
        },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = NavySurface,
            selectedContainerColor = GoldPrimary,
            selectedLabelColor = NavyBackground
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) GoldPrimary else NavySeparator,
            selectedBorderColor = GoldPrimary
        ),
        modifier = Modifier.testTag(testTag)
    )
}

@Composable
private fun BooksListView(
    state: LibrosUiState,
    filtro: FiltroTestamento,
    onLibroClick: (String) -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        is LibrosUiState.Loading -> {
            LoadingState(message = "Cargando sagradas escrituras...")
        }
        is LibrosUiState.Error -> {
            ErrorState(message = state.message, onRetry = onRetry)
        }
        is LibrosUiState.Success -> {
            val filteredBooks = state.libros.filter { libro ->
                when (filtro) {
                    FiltroTestamento.TODOS -> true
                    FiltroTestamento.ANTIGUO -> libro.testamento.equals("Antiguo", ignoreCase = true)
                    FiltroTestamento.NUEVO -> libro.testamento.equals("Nuevo", ignoreCase = true)
                }
            }

            if (filteredBooks.isEmpty()) {
                EmptyState(
                    title = "No se encontraron libros",
                    subtitle = "No hay libros para el filtro seleccionado"
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().testTag("books_lazy_column")
                ) {
                    items(
                        items = filteredBooks,
                        key = { it.libro }
                    ) { libro ->
                        BookItemCard(
                            libro = libro,
                            onClick = { onLibroClick(libro.libro) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookItemCard(
    libro: LibroBiblia,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = NavySurface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GoldSubtle.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .testTag("book_item_${libro.libro}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(NavySurfaceElevated)
                    .border(1.dp, GoldSubtle.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = libro.libro,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "${libro.testamento} Testamento",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldLight.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = GoldLight.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SearchResultsView(
    state: BusquedaUiState,
    onResultClick: (String, Int, Int) -> Unit,
    onLoadMore: () -> Unit
) {
    when (state) {
        is BusquedaUiState.Idle -> {
            EmptyState(
                title = "Buscar versículos",
                subtitle = "Escribe una palabra o frase para buscar en las Escrituras."
            )
        }
        is BusquedaUiState.Searching -> {
            LoadingState(message = "Buscando en la Biblia...")
        }
        is BusquedaUiState.Error -> {
            ErrorState(message = state.message)
        }
        is BusquedaUiState.Success -> {
            if (state.resultados.isEmpty()) {
                EmptyState(
                    title = "Sin resultados",
                    subtitle = "No se encontraron versículos que coincidan con la búsqueda."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().testTag("search_results_lazy_column")
                ) {
                    item {
                        Text(
                            text = if (state.hayMas) {
                                "Mostrando ${state.resultados.size} resultados"
                            } else {
                                "${state.resultados.size} resultados encontrados"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldLight,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(
                        items = state.resultados,
                        key = { it.id }
                    ) { versiculo ->
                        SearchResultItemCard(
                            versiculo = versiculo,
                            onClick = { onResultClick(versiculo.libro, versiculo.capitulo, versiculo.versiculo) }
                        )
                    }
                    if (state.hayMas) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
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
                                    OutlinedButton(
                                        onClick = onLoadMore,
                                        border = BorderStroke(1.dp, GoldPrimary),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = GoldLight
                                        ),
                                        modifier = Modifier.testTag("load_more_button")
                                    ) {
                                        Text("Ver más resultados")
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
private fun SearchResultItemCard(
    versiculo: VersiculoBiblia,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = NavySurface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GoldSubtle.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .testTag("search_result_${versiculo.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = versiculo.referencia,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
                Text(
                    text = "${versiculo.testamento} Testamento",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = versiculo.texto,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Serif,
                color = TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
        }
    }
}
