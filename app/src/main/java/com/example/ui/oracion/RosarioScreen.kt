package com.example.ui.oracion

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.common.LoadingState
import com.example.ui.common.ScreenHeader
import com.example.ui.common.pressable
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtle
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySeparator
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.Radii
import com.example.ui.theme.Spacing
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Rezo guiado del Santo Rosario, espejo del `PrayerRunner` de la RN: cabecera con "Paso X de
 * N" y engranaje de ajustes, cuentas de la decena en curso, texto de la oración centrado y
 * pie con Anterior/Siguiente. Los selectores de misterio, oraciones, avance y tamaño viven en
 * una hoja inferior para dejar la oración casi a pantalla completa, como Rosarium. Queda
 * fuera de la barra de pestañas, como en la RN.
 */
@Composable
fun RosarioScreen(
    viewModel: RosarioViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fontMultiplier by viewModel.fontSizeMultiplier.collectAsStateWithLifecycle()
    val paso = uiState.paso
    var mostrarAjustes by rememberSaveable { mutableStateOf(false) }

    // La RN vuelve a montar la pantalla al entrar: regenera los pasos del día y arranca de
    // cero si el rezo anterior ya se había completado.
    LaunchedEffect(Unit) { viewModel.onScreenEntered() }

    // Al pisar el paso final —avanzando o al reanudar— se registra la racha una sola vez.
    LaunchedEffect(paso?.id) {
        if (paso?.id == RosarioViewModel.PASO_COMPLETADO_ID) {
            viewModel.marcarCompletado()
        }
    }

    // Avance automático: cada paso se muestra el tiempo que lleva rezarlo. El efecto se
    // reinicia al cambiar de paso, de velocidad o de lista (opciones de oración), y se
    // detiene solo al llegar al paso de completado. Con la hoja de ajustes abierta se pausa
    // para que el rezo no siga solo mientras se cambia una opción.
    LaunchedEffect(uiState.pasos, uiState.pasoIndex, uiState.autoavance, mostrarAjustes) {
        if (mostrarAjustes) return@LaunchedEffect
        val actual = uiState.paso ?: return@LaunchedEffect
        if (uiState.autoavance == Autoavance.APAGADO ||
            actual.id == RosarioViewModel.PASO_COMPLETADO_ID
        ) {
            return@LaunchedEffect
        }
        delay(duracionPasoSegundos(actual, uiState.autoavance) * 1000L)
        viewModel.siguiente()
    }

    // Con el avance activo la pantalla no debe apagarse mientras se reza.
    val view = LocalView.current
    DisposableEffect(uiState.autoavance) {
        view.keepScreenOn = uiState.autoavance != Autoavance.APAGADO
        onDispose { view.keepScreenOn = false }
    }

    if (uiState.isLoading || paso == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NavyBackground)
                .testTag("screen_rosario")
        ) {
            LoadingState()
        }
        return
    }

    val misterioActual = paso.mysteryIndex ?: -1
    val avesCompletadas = uiState.pasos
        .take(uiState.pasoIndex + 1)
        .count { it.mysteryIndex == misterioActual && (it.hailMaryIndex ?: 0) > 0 }
    val mostrarCuentas = misterioActual >= 0 && paso.id != RosarioViewModel.PASO_COMPLETADO_ID

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .testTag("screen_rosario")
    ) {
        ScreenHeader(
            title = "Santo Rosario",
            subtitle = buildString {
                append("Paso ${minOf(uiState.pasoIndex + 1, uiState.total)} de ${uiState.total}")
                // Indicador discreto: solo aparece cuando el avance automático está activo.
                if (uiState.autoavance != Autoavance.APAGADO) append(" · Auto")
            },
            showBack = true,
            onBack = onBackClick,
            rightSlot = {
                BotonAjustesRosario(onClick = { mostrarAjustes = true })
            }
        )

        if (mostrarCuentas) {
            CuentasDecena(
                decada = misterioActual,
                label = paso.subtitle.orEmpty(),
                completadas = avesCompletadas
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.xxl, end = Spacing.xxl, top = Spacing.xl, bottom = Spacing.xxxl),
            verticalArrangement = Arrangement.Center
        ) {
            Crossfade(
                targetState = paso,
                animationSpec = tween(durationMillis = 350),
                label = "paso"
            ) { step ->
                ContenidoPaso(
                    paso = step,
                    fontMultiplier = fontMultiplier,
                    mostrarSubtitulo = !mostrarCuentas
                )
            }
        }

        FooterRosario(
            pasoId = paso.id,
            pasoIndex = uiState.pasoIndex,
            onAnterior = viewModel::anterior,
            onSiguiente = viewModel::siguiente,
            onRezarDeNuevo = viewModel::rezarDeNuevo
        )
    }

    if (mostrarAjustes) {
        AjustesRosarioSheet(
            idioma = uiState.idioma,
            misterioManual = uiState.misterioManual,
            oraciones = uiState.oraciones,
            autoavance = uiState.autoavance,
            fontMultiplier = fontMultiplier,
            advertirReinicio = uiState.pasoIndex > 0,
            onIdioma = viewModel::setIdioma,
            onMisterio = viewModel::setMisterioManual,
            onOracion = viewModel::setOracionOpcional,
            onAutoavance = viewModel::setAutoavance,
            onDecreaseFont = viewModel::decreaseFontSize,
            onIncreaseFont = viewModel::increaseFontSize,
            onResetFont = viewModel::resetFontSize,
            onDismiss = { mostrarAjustes = false }
        )
    }
}

/** Engranaje de la cabecera: abre la hoja de ajustes. Área táctil de 48 dp. */
@Composable
private fun BotonAjustesRosario(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .pressable(pressedAlpha = 0.7f, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "Ajustes del rosario" }
            .testTag("rosario_ajustes"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(NavySurfaceElevated, CircleShape)
                .border(1.dp, GoldSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "⚙", color = GoldPrimary, fontSize = 18.sp)
        }
    }
}

/** Diámetro máximo de cada cuenta de la decena; se reduce si la fila no cabe. */
private val TAMANO_CUENTA_MAX = 18.dp

/** Separación mínima y máxima entre cuentas; el hilo asoma en estos tramos. */
private val SEPARACION_CUENTA_MIN = 4.dp
private val SEPARACION_CUENTA_MAX = 10.dp

/** Grosor del hilo que une las cuentas. */
private val GROSOR_HILO = 2.dp

/**
 * Cuentas de la decena en curso: el nombre del misterio ("Misterio N de 5 · Gozosos"), el hilo
 * que une las cuentas y diez cuentas que se encienden con cada avemaría rezada. El hilo se
 * enciende de centro a centro; al cambiar de decena el bloque entra con un desplazamiento corto
 * y las cuentas se apagan. El fundido de color de las cuentas es el único guiño fuera de la RN,
 * inspirado en las transiciones entre decenas de Rosarium.
 */
@Composable
private fun CuentasDecena(decada: Int, label: String, completadas: Int) {
    // La primera decena no entra animada; las siguientes aparecen con un desplazamiento corto
    // que acompaña el cambio de hilo y de cuentas. Con "reducir movimiento" del sistema la
    // escala de duración es 0 y el cambio es instantáneo.
    val decadaInicial = remember { decada }
    val entrada = remember(decada) {
        Animatable(if (decada == decadaInicial) 1f else 0f)
    }
    LaunchedEffect(entrada) {
        if (entrada.value < 1f) {
            entrada.animateTo(1f, animationSpec = tween(durationMillis = 280))
        }
    }

    val fraccion by animateFloatAsState(
        targetValue = fraccionHiloDecena(completadas),
        animationSpec = tween(durationMillis = 280),
        label = "hilo"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = 0.35f + 0.65f * entrada.value
                translationX = (1f - entrada.value) * Spacing.md.toPx()
            }
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = NavySeparator,
                    start = Offset(0f, size.height - stroke / 2f),
                    end = Offset(size.width, size.height - stroke / 2f),
                    strokeWidth = stroke
                )
            }
            .padding(vertical = Spacing.sm)
            .testTag("rosario_cuentas"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = GoldLight,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(Spacing.xs))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl),
            contentAlignment = Alignment.Center
        ) {
            // El diámetro se ajusta al ancho disponible para que las diez cuentas llenen la
            // fila sin apretarse en pantallas angostas ni quedar perdidas en las anchas.
            val tamano = minOf(
                TAMANO_CUENTA_MAX,
                (maxWidth - SEPARACION_CUENTA_MIN * (CUENTAS_POR_DECENA - 1)) / CUENTAS_POR_DECENA
            )
            val separacion = ((maxWidth - tamano * CUENTAS_POR_DECENA) / (CUENTAS_POR_DECENA - 1))
                .coerceIn(SEPARACION_CUENTA_MIN, SEPARACION_CUENTA_MAX)

            Row(
                modifier = Modifier
                    .drawBehind {
                        val diametro = tamano.toPx()
                        val y = size.height / 2f
                        val xInicio = diametro / 2f
                        val xFin = size.width - diametro / 2f
                        val grosor = GROSOR_HILO.toPx()

                        // Hilo apagado: marca el recorrido completo y se ve entre las cuentas.
                        drawLine(
                            color = GoldSubtle.copy(alpha = 0.4f),
                            start = Offset(xInicio, y),
                            end = Offset(xFin, y),
                            strokeWidth = grosor,
                            cap = StrokeCap.Round
                        )
                        // Hilo encendido: crece hasta la última avemaría completada.
                        if (fraccion > 0f) {
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(GoldSubtle, GoldLight),
                                    startX = xInicio,
                                    endX = xFin
                                ),
                                start = Offset(xInicio, y),
                                end = Offset(xInicio + (xFin - xInicio) * fraccion, y),
                                strokeWidth = grosor,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    .semantics {
                        contentDescription =
                            "Cuentas de la decena: $completadas de $CUENTAS_POR_DECENA"
                    }
                    .testTag("rosario_hilo"),
                horizontalArrangement = Arrangement.spacedBy(separacion),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(CUENTAS_POR_DECENA) { indice ->
                    val activa = indice < completadas
                    // La última cuenta rezada se destaca con un leve realce.
                    val actual = indice == completadas - 1
                    val colorFondo by animateColorAsState(
                        targetValue = if (activa) GoldPrimary else NavySurfaceElevated,
                        animationSpec = tween(durationMillis = 200),
                        label = "cuentaFondo"
                    )
                    val colorBorde by animateColorAsState(
                        targetValue = if (activa) GoldLight else GoldSubtle,
                        animationSpec = tween(durationMillis = 200),
                        label = "cuentaBorde"
                    )
                    val escala by animateFloatAsState(
                        targetValue = if (actual) 1.2f else 1f,
                        animationSpec = tween(durationMillis = 200),
                        label = "cuentaEscala"
                    )

                    Box(
                        modifier = Modifier
                            .size(tamano)
                            .graphicsLayer {
                                scaleX = escala
                                scaleY = escala
                            }
                            .clip(CircleShape)
                            .background(colorFondo)
                            .border(if (actual) 2.dp else 1.dp, colorBorde, CircleShape)
                            .semantics {
                                selected = activa
                                contentDescription = "Ave María ${indice + 1} de $CUENTAS_POR_DECENA"
                            }
                            .testTag("rosario_cuenta")
                    )
                }
            }
        }
    }
}

/** Badge con el título del paso, subtítulo opcional y el texto de la oración. */
@Composable
private fun ContenidoPaso(
    paso: RosarioStep,
    fontMultiplier: Float,
    mostrarSubtitulo: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .background(GoldSubtle, RoundedCornerShape(Radii.md))
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
        ) {
            Text(
                text = paso.title,
                color = GoldLight,
                fontSize = (13f * fontMultiplier).sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(Modifier.height(Spacing.lg))

        if (paso.subtitle != null && mostrarSubtitulo) {
            Text(
                text = paso.subtitle,
                color = TextSecondary,
                fontSize = (12f * fontMultiplier).sp,
                fontStyle = FontStyle.Italic
            )
            Spacer(Modifier.height(Spacing.md))
        }

        Text(
            text = paso.text,
            color = TextPrimary,
            fontSize = (17f * fontMultiplier).sp,
            lineHeight = (30f * fontMultiplier).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FooterRosario(
    pasoId: String,
    pasoIndex: Int,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    onRezarDeNuevo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavyBackground)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = GoldSubtle,
                    start = Offset(0f, stroke / 2f),
                    end = Offset(size.width, stroke / 2f),
                    strokeWidth = stroke
                )
            }
            .padding(horizontal = Spacing.xl, vertical = Spacing.md)
    ) {
        if (pasoId != RosarioViewModel.PASO_COMPLETADO_ID) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                if (pasoIndex > 0) {
                    BotonRosario(
                        text = "← Anterior",
                        principal = false,
                        onClick = onAnterior,
                        testTag = "rosario_prev",
                        modifier = Modifier.weight(1f)
                    )
                }
                BotonRosario(
                    text = "Amén · Siguiente",
                    principal = true,
                    onClick = onSiguiente,
                    testTag = "rosario_next",
                    // El botón principal domina el footer: ocupa todo el ancho en el primer
                    // paso y el doble que "← Anterior" cuando ambos conviven.
                    modifier = if (pasoIndex > 0) Modifier.weight(2f) else Modifier.fillMaxWidth()
                )
            }
        } else {
            BotonRosario(
                text = "Rezar de nuevo",
                principal = true,
                onClick = onRezarDeNuevo,
                testTag = "rosario_restart"
            )
        }
    }
}

@Composable
private fun BotonRosario(
    text: String,
    principal: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(Radii.lg)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (principal) GoldSubtle else NavySurfaceElevated)
            .border(1.dp, if (principal) GoldPrimary else GoldSubtle, shape)
            .pressable(pressedAlpha = 0.8f, onClick = onClick)
            .padding(vertical = Spacing.lg)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (principal) GoldLight else TextSecondary,
            fontSize = 16.sp,
            fontWeight = if (principal) FontWeight.Bold else FontWeight.SemiBold,
            letterSpacing = if (principal) 0.5.sp else 0.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
