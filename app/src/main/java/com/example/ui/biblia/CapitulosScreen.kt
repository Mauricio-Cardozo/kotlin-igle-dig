package com.example.ui.biblia

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.common.EmptyState
import com.example.ui.common.ErrorState
import com.example.ui.common.LoadingState
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtle
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapitulosScreen(
    libro: String,
    viewModel: BibliaViewModel,
    onCapituloClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(libro) {
        viewModel.loadCapitulos(libro)
    }

    val state by viewModel.capitulosState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = libro,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.Serif,
                            color = GoldLight,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Selecciona un capítulo",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("capitulos_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a libros",
                            tint = GoldLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyBackground,
                    titleContentColor = GoldLight
                )
            )
        },
        containerColor = NavyBackground,
        modifier = modifier.testTag("screen_capitulos")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val s = state) {
                is CapitulosUiState.Loading -> {
                    LoadingState(message = "Cargando capítulos de $libro...")
                }
                is CapitulosUiState.Error -> {
                    ErrorState(
                        message = s.message,
                        onRetry = { viewModel.loadCapitulos(libro) }
                    )
                }
                is CapitulosUiState.Success -> {
                    if (s.capitulos.isEmpty()) {
                        EmptyState(
                            title = "Sin capítulos",
                            subtitle = "No se encontraron capítulos para este libro."
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 64.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("capitulos_grid")
                        ) {
                            items(
                                items = s.capitulos,
                                key = { it }
                            ) { capitulo ->
                                CapituloGridItem(
                                    capitulo = capitulo,
                                    onClick = { onCapituloClick(capitulo) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CapituloGridItem(
    capitulo: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = NavySurface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .aspectRatio(1f)
            .border(1.dp, GoldSubtle.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .testTag("capitulo_item_$capitulo")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$capitulo",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary
            )
        }
    }
}
