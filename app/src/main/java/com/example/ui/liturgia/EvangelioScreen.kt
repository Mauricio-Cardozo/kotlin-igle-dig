package com.example.ui.liturgia

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.Lectura
import com.example.data.user.Favorito
import com.example.ui.common.ScreenHeader
import com.example.ui.common.pressable
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.Radii
import com.example.ui.theme.Spacing
import com.example.ui.theme.TextPrimary
import com.example.util.formatoFecha

/**
 * Pantalla "Evangelio del Día": muestra las lecturas litúrgicas de una fecha
 * (o de hoy si [fecha] es null), en paridad visual con la RN — secciones
 * planas sin tarjeta, header con compartir ↗ y favorito ❤️, y estados
 * simples de carga/error.
 */
@Composable
fun EvangelioScreen(
    viewModel: LiturgiaViewModel,
    fecha: String?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(fecha) {
        viewModel.loadEvangelio(fecha)
    }

    val state by viewModel.evangelioState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .testTag("screen_evangelio")
    ) {
        val lectura = state.lectura
        when {
            state.isLoading -> EvangelioLoadingState()
            lectura == null -> EvangelioErrorState(
                message = state.errorMessage ?: "No hay lecturas para hoy.",
                onRetry = { viewModel.loadEvangelio(fecha) }
            )
            else -> {
                ScreenHeader(
                    title = "Evangelio del Día",
                    subtitle = runCatching { formatoFecha(lectura.fecha) }
                        .getOrDefault(lectura.fecha),
                    showBack = true,
                    onBack = onBackClick,
                    rightSlot = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ShareButton(onClick = { compartirLecturas(context, lectura) })
                            FavButton(
                                favorito = state.favorito,
                                onClick = { viewModel.toggleEvangelioFavorito() }
                            )
                        }
                    }
                )
                EvangelioContent(lectura)
            }
        }
    }
}

/** Spinner simple centrado, como el `ActivityIndicator` de la RN. */
@Composable
private fun EvangelioLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = GoldPrimary)
    }
}

/** Estado vacío/error de la RN: texto `#E07070` + botón "Reintentar". */
@Composable
private fun EvangelioErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = ErrorColor,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = Spacing.lg)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Radii.lg))
                .background(GoldPrimary)
                .pressable(pressedAlpha = 0.7f, onClick = onRetry)
                .padding(horizontal = Spacing.xxl, vertical = 10.dp)
                .testTag("evangelio_reintentar_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Reintentar",
                color = NavyBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Compartir `↗` del header, igual que la RN (padding 4, dorado 20). */
@Composable
private fun ShareButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .pressable(pressedAlpha = 0.7f, onClick = onClick)
            .padding(Spacing.xs)
            .testTag("evangelio_share_button"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "↗",
            color = GoldPrimary,
            fontSize = 20.sp,
            lineHeight = 20.sp
        )
    }
}

/**
 * Corazón de favorito del header, como el `FavBtn` de la RN: ❤️ guardado,
 * 🤍 no guardado, y punto de 8 dp con el color del favorito si lo tiene.
 */
@Composable
private fun FavButton(favorito: Favorito?, onClick: () -> Unit) {
    val activo = favorito != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .pressable(pressedAlpha = 0.7f, onClick = onClick)
            .padding(Spacing.xs)
            .semantics {
                contentDescription = if (activo) "Quitar de favoritos" else "Añadir a favoritos"
            }
            .testTag("evangelio_fav_button")
    ) {
        favorito?.color?.let { hex ->
            Box(
                modifier = Modifier
                    .padding(end = Spacing.xs)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(parseFavoriteColor(hex))
            )
        }
        Text(
            text = if (activo) "❤️" else "🤍",
            fontSize = 20.sp,
            lineHeight = 24.sp
        )
    }
}

/** Color del punto del favorito; si el hex guardado es inválido cae al dorado. */
private fun parseFavoriteColor(colorHex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(GoldPrimary)

@Composable
private fun EvangelioContent(lectura: Lectura) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = Spacing.massive),
        modifier = Modifier
            .fillMaxSize()
            .testTag("evangelio_lazy_column")
    ) {
        if (!lectura.tituloMisa.isNullOrBlank()) {
            item {
                Text(
                    text = lectura.tituloMisa,
                    color = GoldLight,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Spacing.lg,
                            end = Spacing.lg,
                            top = Spacing.xl,
                            bottom = Spacing.sm
                        )
                        .testTag("evangelio_titulo_misa")
                )
            }
        }

        // Orden del Misal (decisión de la auditoría): 1ª → Salmo → 2ª →
        // Aleluya → Evangelio → Comentario. La RN muestra la 2ª antes del
        // salmo; no se copia ese error.
        if (!lectura.primeraLectura.isNullOrBlank()) {
            item {
                ReadingSection("Primera Lectura", lectura.primeraLecturaRef, lectura.primeraLectura)
            }
        }
        if (!lectura.salmo.isNullOrBlank()) {
            item { ReadingSection("Salmo Responsorial", null, lectura.salmo) }
        }
        if (!lectura.segundaLectura.isNullOrBlank()) {
            item {
                ReadingSection("Segunda Lectura", lectura.segundaLecturaRef, lectura.segundaLectura)
            }
        }
        if (!lectura.aleluia.isNullOrBlank()) {
            item { ReadingSection("Aleluya", null, lectura.aleluia) }
        }
        if (!lectura.evangelio.isNullOrBlank()) {
            item { ReadingSection("Evangelio", lectura.evangelioRef, lectura.evangelio) }
        }
        if (!lectura.comentarioPapal.isNullOrBlank()) {
            item { ReadingSection("Comentario", null, lectura.comentarioPapal) }
        }
    }
}

/**
 * Sección de lectura plana (sin tarjeta), como el `ReadingSection` de la RN:
 * label 12/700 ls 1.2, referencia itálica 14 `#E8C97A` y texto 15/24 sans.
 */
@Composable
private fun ReadingSection(
    label: String,
    referencia: String?,
    texto: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.xxl)
    ) {
        Text(
            text = label,
            color = GoldPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        if (!referencia.isNullOrBlank()) {
            Text(
                text = referencia,
                color = GoldLight,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(bottom = Spacing.sm)
            )
        }
        Text(
            text = texto,
            color = TextPrimary,
            fontSize = 15.sp,
            lineHeight = 24.sp
        )
    }
}

/** Comparte las lecturas como la RN: título, 1ª lectura, salmo y evangelio. */
private fun compartirLecturas(context: Context, lectura: Lectura) {
    val partes = buildList {
        val titulo = lectura.tituloMisa
        if (!titulo.isNullOrBlank()) add(titulo)
        if (!lectura.primeraLectura.isNullOrBlank()) {
            add(textoConReferencia(lectura.primeraLecturaRef, lectura.primeraLectura))
        }
        if (!lectura.salmo.isNullOrBlank()) {
            add("Salmo:\n${lectura.salmo}")
        }
        if (!lectura.evangelio.isNullOrBlank()) {
            add(textoConReferencia(lectura.evangelioRef, lectura.evangelio))
        }
    }
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, partes.joinToString("\n\n"))
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Compartir el Evangelio del día"))
}

private fun textoConReferencia(referencia: String?, texto: String): String =
    if (referencia.isNullOrBlank()) texto else "$referencia:\n$texto"
