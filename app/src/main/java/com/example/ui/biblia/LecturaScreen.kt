package com.example.ui.biblia

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.VersiculoBiblia
import com.example.data.user.Favorito
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

// 6 Highlight color palettes (Hex string & Display Color)
data class HighlightColorOption(
    val name: String,
    val hex: String,
    val color: Color
)

val HighlightOptions = listOf(
    HighlightColorOption("Dorado Sacro", "#55C9A84C", Color(0x55C9A84C)),
    HighlightColorOption("Esmeralda Esperanza", "#552E7D32", Color(0x552E7D32)),
    HighlightColorOption("Rubí Pasión", "#55C62828", Color(0x55C62828)),
    HighlightColorOption("Azul Mariano", "#551565C0", Color(0x551565C0)),
    HighlightColorOption("Púrpura Adviento", "#557B1FA2", Color(0x557B1FA2)),
    HighlightColorOption("Ámbar Pentecostés", "#55E65100", Color(0x55E65100))
)

@OptIn(ExperimentalMaterial3Api::class)
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
    LaunchedEffect(libro, capitulo) {
        viewModel.loadLectura(libro, capitulo)
    }

    val state by viewModel.lecturaState.collectAsStateWithLifecycle()
    val fontMultiplier by viewModel.fontSizeMultiplier.collectAsStateWithLifecycle()

    var showFontControls by remember { mutableStateOf(false) }
    var selectedVersiculoForHighlight by remember { mutableStateOf<VersiculoBiblia?>(null) }

    val listState = rememberLazyListState()

    // Scroll to target verse if coming from search
    LaunchedEffect(state, targetVersiculo) {
        if (state is LecturaUiState.Success && targetVersiculo != null) {
            val versiculos = (state as LecturaUiState.Success).versiculos
            val index = versiculos.indexOfFirst { it.versiculo == targetVersiculo }
            if (index >= 0) {
                listState.animateScrollToItem(index)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "$libro $capitulo",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.Serif,
                            color = GoldLight,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Libro de la Sagrada Escritura",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("lectura_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a capítulos",
                            tint = GoldLight
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showFontControls = !showFontControls },
                        modifier = Modifier.testTag("toggle_font_controls_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Ajustar tamaño de fuente",
                            tint = if (showFontControls) GoldPrimary else GoldLight
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
        modifier = modifier.testTag("screen_lectura")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Font Size Adjustment Bar
            if (showFontControls) {
                FontSizeControlBar(
                    currentMultiplier = fontMultiplier,
                    onDecrease = viewModel::decreaseFontSize,
                    onIncrease = viewModel::increaseFontSize,
                    onReset = viewModel::resetFontSize
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (val s = state) {
                    is LecturaUiState.Loading -> {
                        LoadingState(message = "Cargando $libro capítulo $capitulo...")
                    }
                    is LecturaUiState.Error -> {
                        ErrorState(
                            message = s.message,
                            onRetry = { viewModel.loadLectura(libro, capitulo) }
                        )
                    }
                    is LecturaUiState.Success -> {
                        if (s.versiculos.isEmpty()) {
                            EmptyState(
                                title = "Sin versículos",
                                subtitle = "No se encontraron versículos en este capítulo."
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("versiculos_lazy_column")
                            ) {
                                items(
                                    items = s.versiculos,
                                    key = { it.id }
                                ) { versiculo ->
                                    val favKey = "biblia-${versiculo.id}"
                                    val favorito = s.favoritosMap[favKey]

                                    VersiculoCard(
                                        versiculo = versiculo,
                                        favorito = favorito,
                                        fontMultiplier = fontMultiplier,
                                        onLongPress = {
                                            if (favorito != null) {
                                                // Already favorite -> Remove
                                                viewModel.toggleFavorito(versiculo, "")
                                            } else {
                                                // Not favorite -> Open color dialog
                                                selectedVersiculoForHighlight = versiculo
                                            }
                                        },
                                        onShare = {
                                            shareVersiculo(context, versiculo)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Highlight Color Picker Dialog
    if (selectedVersiculoForHighlight != null) {
        val targetV = selectedVersiculoForHighlight!!
        HighlightColorPickerDialog(
            referencia = targetV.referencia,
            onDismiss = { selectedVersiculoForHighlight = null },
            onColorSelect = { hexColor ->
                viewModel.toggleFavorito(targetV, hexColor)
                selectedVersiculoForHighlight = null
            }
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
    Surface(
        color = NavySurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NavySeparator)
            .testTag("font_size_control_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tamaño: ${(currentMultiplier * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // A- Button
                IconButton(
                    onClick = onDecrease,
                    enabled = currentMultiplier > 0.85f,
                    modifier = Modifier.testTag("font_decrease_button")
                ) {
                    Text(
                        text = "A-",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (currentMultiplier > 0.85f) GoldLight else TextSecondary.copy(alpha = 0.4f)
                    )
                }

                // Reset Button
                IconButton(
                    onClick = onReset,
                    modifier = Modifier.testTag("font_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Restablecer tamaño de fuente",
                        tint = GoldPrimary
                    )
                }

                // A+ Button
                IconButton(
                    onClick = onIncrease,
                    enabled = currentMultiplier < 1.45f,
                    modifier = Modifier.testTag("font_increase_button")
                ) {
                    Text(
                        text = "A+",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (currentMultiplier < 1.45f) GoldLight else TextSecondary.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VersiculoCard(
    versiculo: VersiculoBiblia,
    favorito: Favorito?,
    fontMultiplier: Float,
    onLongPress: () -> Unit,
    onShare: () -> Unit
) {
    val highlightColor = remember(favorito?.color) {
        if (!favorito?.color.isNullOrBlank()) {
            try {
                val hexStr = favorito!!.color!!.removePrefix("#")
                val longVal = hexStr.toLong(16)
                Color(longVal)
            } catch (e: Exception) {
                Color(0x55C9A84C)
            }
        } else {
            Color.Transparent
        }
    }

    val baseFontSize = 17.sp * fontMultiplier
    val baseLineHeight = 26.sp * fontMultiplier

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (favorito != null) highlightColor else NavySurface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (favorito != null) 1.5.dp else 1.dp,
                color = if (favorito != null) GoldPrimary else GoldSubtle.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            )
            .testTag("versiculo_item_${versiculo.versiculo}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Verse Number in Sacred Gold
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${versiculo.versiculo}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        fontSize = (16.sp * fontMultiplier)
                    )
                    if (favorito != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Versículo guardado",
                            tint = GoldLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Share Button
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("share_versiculo_${versiculo.versiculo}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartir versículo",
                        tint = GoldLight.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Verse Text in Serif Font
            Text(
                text = versiculo.texto,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Serif,
                fontSize = baseFontSize,
                lineHeight = baseLineHeight,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun HighlightColorPickerDialog(
    referencia: String,
    onDismiss: () -> Unit,
    onColorSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurfaceElevated,
        title = {
            Column {
                Text(
                    text = "Resaltar y Guardar",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = referencia,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Elige un tono para resaltar este versículo en tu Biblia:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HighlightOptions.forEach { opt ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(opt.color)
                                    .border(2.dp, GoldLight, CircleShape)
                                    .clickable { onColorSelect(opt.hex) }
                                    .testTag("highlight_color_${opt.name.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("highlight_dialog_cancel")
            ) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
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
