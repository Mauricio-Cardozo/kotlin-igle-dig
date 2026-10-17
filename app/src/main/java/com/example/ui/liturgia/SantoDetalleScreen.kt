package com.example.ui.liturgia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.MisalSantosEntry
import com.example.ui.common.EmptyState
import com.example.ui.common.LoadingState
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Pantalla de detalle del Santo: biografía + Propio del santo (misal_santos).
 * Reemplaza al ComingSoonScreen (Fase 0.3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SantoDetalleScreen(
    viewModel: LiturgiaViewModel,
    santoId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(santoId) {
        viewModel.loadSantoDetalle(santoId)
    }

    val state by viewModel.santoDetalleState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.santo?.nombre ?: "Santo",
                            style = MaterialTheme.typography.titleLarge,
                            color = GoldLight,
                            fontWeight = FontWeight.Bold
                        )
                        if (!state.santo?.titulo.isNullOrBlank()) {
                            Text(
                                text = state.santo?.titulo.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("santo_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
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
        modifier = modifier.testTag("screen_santo_detalle")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val santo = state.santo
            when {
                state.isLoading -> LoadingState(message = "Cargando la vida del santo...")
                santo == null -> EmptyState(
                    title = "Santo no encontrado",
                    subtitle = "No se pudo cargar la información de este santo."
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("santo_lazy_column")
                ) {
                    if (santo.biografia.isNotBlank()) {
                        item {
                            Text(
                                text = santo.biografia,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .testTag("santo_biografia")
                            )
                        }
                    }

                    if (state.misalSantos.isNotEmpty()) {
                        item {
                            Text(
                                text = "ORACIONES PROPIAS",
                                color = GoldPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(state.misalSantos) { entry -> MisalSantoBlock(entry) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MisalSantoBlock(entry: MisalSantosEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NavySurface)
            .padding(16.dp)
            .padding(bottom = 4.dp)
    ) {
        if (!entry.nombre.isNullOrBlank()) {
            Text(
                text = entry.nombre,
                color = GoldLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        if (!entry.rango.isNullOrBlank()) {
            Text(
                text = entry.rango,
                color = TextSecondary,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        if (!entry.antifonaEntrada.isNullOrBlank()) {
            PrayerCard(label = "Antífona de entrada", text = entry.antifonaEntrada)
        }
        if (!entry.colecta.isNullOrBlank()) {
            PrayerCard(label = "Oración colecta", text = entry.colecta)
        }
        if (!entry.oracionOfrendas.isNullOrBlank()) {
            PrayerCard(label = "Oración sobre las ofrendas", text = entry.oracionOfrendas)
        }
        if (!entry.prefacio.isNullOrBlank()) {
            PrayerCard(label = "Prefacio", text = entry.prefacio)
        }
        if (!entry.antifonaComunion.isNullOrBlank()) {
            PrayerCard(label = "Antífona de comunión", text = entry.antifonaComunion)
        }
        if (!entry.postcomunion.isNullOrBlank()) {
            PrayerCard(label = "Postcomunión", text = entry.postcomunion)
        }
    }
}
