package com.example.ui.biblia

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.VersiculoBiblia
import com.example.data.user.Favorito
import com.example.ui.common.ErrorState
import com.example.ui.common.FontSizeControl
import com.example.ui.common.LoadingState
import com.example.ui.common.ScreenHeader
import com.example.ui.common.pressable
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtle
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySeparator
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.Radii
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Opciones de resaltado, en el mismo orden de la RN. El favorito persiste el
 * hex con alfa 0x30 (para el fondo del versículo) y el swatch se muestra con
 * el tono opaco, como en el modal de referencia.
 */
data class HighlightColorOption(
    val name: String,
    val hex: String,
    val color: Color,
    val swatch: Color
)

val HighlightOptions = listOf(
    HighlightColorOption("Dorado Sacro", "#30E8C97A", Color(0x30E8C97A), Color(0xFFE8C97A)),
    HighlightColorOption("Esmeralda Esperanza", "#304CAF50", Color(0x304CAF50), Color(0xFF4CAF50)),
    HighlightColorOption("Púrpura Adviento", "#307B3FAF", Color(0x307B3FAF), Color(0xFF7B3FAF)),
    HighlightColorOption("Azul Mariano", "#302196F3", Color(0x302196F3), Color(0xFF2196F3)),
    HighlightColorOption("Rubí Pasión", "#30E07070", Color(0x30E07070), Color(0xFFE07070)),
    HighlightColorOption("Ámbar Pentecostés", "#30FF9800", Color(0x30FF9800), Color(0xFFFF9800))
)

/**
 * Nivel 3 del flujo bíblico: lector de filas planas (sin tarjeta), con número
 * inline, compartir, corazón de favorito y resaltado con alfa 0x30, en paridad
 * con la RN. Conserva el control de tamaño de fuente propio.
 */
@Composable
fun LecturaScreen(
    libro: String,
    capitulo: Int,
    targetVersiculo: Int?,
    viewModel: BibliaViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(libro, capitulo) { viewModel.loadLectura(libro, capitulo) }

    val state by viewModel.lecturaState.collectAsStateWithLifecycle()
    val fontMultiplier by viewModel.fontSizeMultiplier.collectAsStateWithLifecycle()
    val bibliaVersion by viewModel.bibliaVersion.collectAsStateWithLifecycle()

    var showFontControls by remember { mutableStateOf(false) }
    var selectedVersiculoForHighlight by remember { mutableStateOf<VersiculoBiblia?>(null) }
    val listState = rememberLazyListState()

    // Scroll al versículo objetivo si se llegó desde la búsqueda.
    LaunchedEffect(state, targetVersiculo) {
        if (state is LecturaUiState.Success && targetVersiculo != null) {
            val versiculos = (state as LecturaUiState.Success).versiculos
            val index = versiculos.indexOfFirst { it.versiculo == targetVersiculo }
            if (index >= 0) {
                listState.animateScrollToItem(index)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .testTag("screen_lectura")
    ) {
        ScreenHeader(
            title = "$libro $capitulo",
            superLabel = "✝ BIBLIA ${bibliaVersion.label.uppercase()}",
            subtitle = "$libro › Cap. $capitulo",
            showBack = true,
            onBack = onBackClick,
            rightSlot = {
                // Extra propio (no está en la RN): control de tamaño de fuente.
                FontSizeToggleButton(
                    expanded = showFontControls,
                    onClick = { showFontControls = !showFontControls }
                )
            }
        )

        if (showFontControls) {
            FontSizeControlBar(
                currentMultiplier = fontMultiplier,
                onDecrease = viewModel::decreaseFontSize,
                onIncrease = viewModel::increaseFontSize,
                onReset = viewModel::resetFontSize
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (val s = state) {
                is LecturaUiState.Loading -> LoadingState(message = "Cargando…")
                is LecturaUiState.Error -> ErrorState(
                    message = s.message,
                    onRetry = { viewModel.loadLectura(libro, capitulo) }
                )
                is LecturaUiState.Success -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("versiculos_lazy_column")
                ) {
                    items(items = s.versiculos, key = { it.id }) { versiculo ->
                        val favKey = "${bibliaVersion.favoritoPrefix}${versiculo.id}"
                        val favorito = s.favoritosMap[favKey]
                        VersiculoRow(
                            versiculo = versiculo,
                            favorito = favorito,
                            fontMultiplier = fontMultiplier,
                            onLongPress = {
                                if (favorito != null) {
                                    // Ya guardado: el long press lo quita.
                                    viewModel.toggleFavorito(versiculo, "")
                                } else {
                                    selectedVersiculoForHighlight = versiculo
                                }
                            },
                            onShare = { shareVersiculo(context, versiculo) }
                        )
                    }
                }
            }
        }
    }

    // Modal de resaltado (mismo flujo que la RN).
    selectedVersiculoForHighlight?.let { target ->
        HighlightColorPickerDialog(
            onDismiss = { selectedVersiculoForHighlight = null },
            onColorSelect = { hexColor ->
                viewModel.toggleFavorito(target, hexColor)
                selectedVersiculoForHighlight = null
            }
        )
    }
}

@Composable
private fun FontSizeToggleButton(expanded: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radii.md)
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(shape)
            .background(NavySurfaceElevated)
            .border(1.dp, if (expanded) GoldPrimary else GoldSubtle, shape)
            .pressable(pressedAlpha = 0.7f, onClick = onClick)
            .testTag("toggle_font_controls_button"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Aa",
            color = GoldPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FontSizeControlBar(
    currentMultiplier: Float,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onReset: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavySurface)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = NavySeparator,
                    start = Offset(0f, size.height - stroke / 2f),
                    end = Offset(size.width, size.height - stroke / 2f),
                    strokeWidth = stroke
                )
            }
            .testTag("font_size_control_bar")
    ) {
        FontSizeControl(
            multiplier = currentMultiplier,
            onDecrease = onDecrease,
            onIncrease = onIncrease,
            onReset = onReset,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VersiculoRow(
    versiculo: VersiculoBiblia,
    favorito: Favorito?,
    fontMultiplier: Float,
    onLongPress: () -> Unit,
    onShare: () -> Unit
) {
    val highlightColor = remember(favorito?.color) {
        val hex = favorito?.color?.removePrefix("#")
        if (!hex.isNullOrBlank()) {
            try {
                // Los favoritos guardan el alfa 0x30 al frente ("#30RRGGBB").
                val argb = if (hex.length == 6) "30$hex" else hex
                Color(argb.toLong(16))
            } catch (e: NumberFormatException) {
                Color(0x30E8C97A)
            }
        } else {
            Color.Transparent
        }
    }
    val heartColor = parseOpaqueHex(favorito?.color) ?: ErrorColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(highlightColor)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onLongPress
            )
            .testTag("versiculo_item_${versiculo.versiculo}"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "${versiculo.versiculo}",
            color = GoldPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier
                .widthIn(min = 28.dp)
                .padding(top = 2.dp)
        )
        Text(
            text = versiculo.texto,
            color = TextPrimary,
            fontSize = 15.sp * fontMultiplier,
            lineHeight = 24.sp * fontMultiplier,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "↗",
            color = TextSecondary,
            fontSize = 16.sp,
            modifier = Modifier
                .pressable(pressedAlpha = 0.7f, onClick = onShare)
                .padding(horizontal = 4.dp)
                .testTag("share_versiculo_${versiculo.versiculo}")
        )
        Text(
            text = if (favorito != null) "♥" else "",
            color = heartColor,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(20.dp)
                .padding(top = 2.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HighlightColorPickerDialog(
    onDismiss: () -> Unit,
    onColorSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurfaceElevated,
        shape = RoundedCornerShape(Radii.xl),
        title = {
            Text(
                text = "Resaltar versículo",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HighlightOptions.forEach { opt ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(opt.swatch, CircleShape)
                            .pressable(pressedAlpha = 0.7f, onClick = { onColorSelect(opt.hex) })
                            .testTag("highlight_color_${opt.name.lowercase().replace(" ", "_")}")
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .pressable(pressedAlpha = 0.7f, onClick = onDismiss)
                        .testTag("highlight_dialog_cancel"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = TextSecondary,
                        fontSize = 18.sp
                    )
                }
            }
        },
        confirmButton = {}
    )
}

/** Toma el color opaco (sin el alfa 0x30) del hex guardado en el favorito. */
private fun parseOpaqueHex(colorHex: String?): Color? {
    val cleaned = colorHex?.removePrefix("#")?.takeLast(6) ?: return null
    if (cleaned.length != 6) return null
    return try {
        Color(0xFF000000L or cleaned.toLong(16))
    } catch (e: NumberFormatException) {
        null
    }
}

private fun shareVersiculo(context: Context, versiculo: VersiculoBiblia) {
    // Formato exacto requerido: "texto — Libro cap:vers"
    val shareText = "${versiculo.texto} — ${versiculo.libro} ${versiculo.capitulo}:${versiculo.versiculo}"
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Compartir versículo bíblico")
    context.startActivity(shareIntent)
}
