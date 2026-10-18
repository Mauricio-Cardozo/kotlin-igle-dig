package com.example.ui.biblia

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.common.ErrorState
import com.example.ui.common.LoadingState
import com.example.ui.common.ScreenHeader
import com.example.ui.common.pressable
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldSubtle
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurface
import com.example.ui.theme.Radii

/**
 * Nivel 2 del flujo bíblico: grilla fija de 5 columnas con las celdas de
 * capítulo de la RN (48 dp, radio 8, borde dorado tenue, número 15/700).
 */
@Composable
fun CapitulosScreen(
    libro: String,
    viewModel: BibliaViewModel,
    onCapituloClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(libro) { viewModel.loadCapitulos(libro) }

    val state by viewModel.capitulosState.collectAsStateWithLifecycle()
    val version by viewModel.bibliaVersion.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .testTag("screen_capitulos")
    ) {
        ScreenHeader(
            title = libro,
            superLabel = "✝ BIBLIA ${version.label.uppercase()}",
            subtitle = testamentoDe(libro)?.let { "$it Testamento" },
            showBack = true,
            onBack = onBackClick
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when (val s = state) {
                is CapitulosUiState.Loading -> LoadingState(message = "Cargando…")
                is CapitulosUiState.Error -> ErrorState(
                    message = s.message,
                    onRetry = { viewModel.loadCapitulos(libro) }
                )
                is CapitulosUiState.Success -> LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("capitulos_grid")
                ) {
                    items(items = s.capitulos, key = { it }) { capitulo ->
                        CapituloCell(capitulo = capitulo, onClick = { onCapituloClick(capitulo) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CapituloCell(capitulo: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radii.md)
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(shape)
            .background(NavySurface)
            .border(1.dp, GoldSubtle, shape)
            .pressable(pressedAlpha = 0.75f, onClick = onClick)
            .testTag("capitulo_item_$capitulo"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$capitulo",
            color = GoldLight,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
