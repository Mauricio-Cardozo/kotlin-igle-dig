package com.example.ui.liturgia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.common.EmptyState
import com.example.ui.common.LoadingState
import com.example.ui.common.pressable
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.Spacing
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Pantalla de detalle del Santo: biografía + Propio del santo (misal_santos),
 * en paridad visual con `app/santo/[id].tsx` de la RN: header con back circular
 * de 36 dp y título de 20 centrado, subtítulo itálico en el cuerpo y oraciones
 * sin tarjeta anidada.
 */
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .testTag("screen_santo_detalle")
    ) {
        // Header de la RN: back circular 36 con ‹ y título 20 centrado.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NavySurfaceElevated)
                    .pressable(pressedAlpha = 0.7f, onClick = onBackClick)
                    .testTag("santo_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "‹",
                    color = GoldPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = (-2).dp)
                )
            }
            Text(
                text = state.santo?.nombre ?: "Santo",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(40.dp))
        }

        val santo = state.santo
        when {
            state.isLoading -> LoadingState(message = "Cargando la vida del santo...")
            santo == null -> EmptyState(
                message = "No se pudo cargar la información de este santo."
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(start = Spacing.xl, end = Spacing.xl, bottom = 60.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("santo_lazy_column")
            ) {
                // El subtítulo va en el cuerpo (itálico dorado), como la RN.
                val cleanTitulo = limpiarTitulo(santo.titulo)
                if (cleanTitulo != null) {
                    item {
                        Text(
                            text = cleanTitulo,
                            color = GoldLight,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(bottom = Spacing.lg)
                        )
                    }
                }

                item {
                    if (santo.biografia.isNotBlank()) {
                        Text(
                            text = santo.biografia,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            modifier = Modifier
                                .padding(bottom = Spacing.xl)
                                .testTag("santo_biografia")
                        )
                    } else {
                        Text(
                            text = "No hay biografía disponible para este santo.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier
                                .padding(bottom = Spacing.xl)
                                .testTag("santo_biografia")
                        )
                    }
                }

                // Oraciones propias: una tarjeta por oración (sin card anidada).
                val propio = state.misalSantos.firstOrNull { !it.colecta.isNullOrBlank() }
                    ?: state.misalSantos.firstOrNull()
                if (propio != null) {
                    item {
                        Text(
                            text = "ORACIONES PROPIAS",
                            color = GoldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.md)
                        )
                    }
                    item {
                        Column {
                            if (!propio.antifonaEntrada.isNullOrBlank()) {
                                PrayerCard(label = "Antífona de entrada", text = propio.antifonaEntrada)
                            }
                            if (!propio.colecta.isNullOrBlank()) {
                                PrayerCard(label = "Oración colecta", text = propio.colecta)
                            }
                            if (!propio.oracionOfrendas.isNullOrBlank()) {
                                PrayerCard(label = "Oración sobre las ofrendas", text = propio.oracionOfrendas)
                            }
                            if (!propio.prefacio.isNullOrBlank()) {
                                PrayerCard(label = "Prefacio", text = propio.prefacio)
                            }
                            if (!propio.antifonaComunion.isNullOrBlank()) {
                                PrayerCard(label = "Antífona de comunión", text = propio.antifonaComunion)
                            }
                            if (!propio.postcomunion.isNullOrBlank()) {
                                PrayerCard(label = "Postcomunión", text = propio.postcomunion)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Quita el rango final («Memoria», «Fiesta», «Solemnidad») del título, como la RN. */
private fun limpiarTitulo(titulo: String?): String? =
    titulo
        ?.replace(Regex("[.\\s]*(Memoria|Fiesta|Solemnidad)$", RegexOption.IGNORE_CASE), "")
        ?.replace(Regex("[.\\s]+$"), "")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
