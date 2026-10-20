package com.example.ui.oracion

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.common.FontSizeControl
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
 * N" y control de tamaño, cuentas de la decena en curso, texto de la oración centrado y pie
 * con Anterior/Siguiente. Queda fuera de la barra de pestañas, como en la RN.
 */
@Composable
fun RosarioScreen(
    viewModel: RosarioViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val fontMultiplier by viewModel.fontSizeMultiplier.collectAsStateWithLifecycle()
    val paso = uiState.paso

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
    // detiene solo al llegar al paso de completado.
    LaunchedEffect(uiState.pasos, uiState.pasoIndex, uiState.autoavance) {
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
            subtitle = "Paso ${minOf(uiState.pasoIndex + 1, uiState.total)} de ${uiState.total}",
            showBack = true,
            onBack = onBackClick,
            rightSlot = {
                FontSizeControl(
                    multiplier = fontMultiplier,
                    onDecrease = viewModel::decreaseFontSize,
                    onIncrease = viewModel::increaseFontSize,
                    onReset = viewModel::resetFontSize
                )
            }
        )

        SelectorMisterio(
            seleccionado = uiState.misterioManual,
            onSeleccion = viewModel::setMisterioManual
        )

        SelectorOraciones(
            oraciones = uiState.oraciones,
            onToggle = viewModel::setOracionOpcional,
            advertirReinicio = uiState.pasoIndex > 0
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
            autoavance = uiState.autoavance,
            onAutoavance = viewModel::setAutoavance,
            onAnterior = viewModel::anterior,
            onSiguiente = viewModel::siguiente,
            onRezarDeNuevo = viewModel::rezarDeNuevo
        )
    }
}

/**
 * Selector manual de misterios: "Auto" sigue la regla litúrgica por fecha y las otras cuatro
 * opciones fijan la decena del día. Cambiar la selección reinicia el rezo (los pasos cambian).
 */
@Composable
private fun SelectorMisterio(
    seleccionado: MisterioRosario,
    onSeleccion: (MisterioRosario) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl, vertical = Spacing.sm)
            .testTag("rosario_misterios"),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        MisterioRosario.entries.forEach { opcion ->
            PillRosario(
                texto = opcion.etiqueta,
                activa = opcion == seleccionado,
                testTag = "rosario_misterio_${opcion.name.lowercase()}",
                onClick = { onSeleccion(opcion) }
            )
        }
    }
}

/**
 * Oraciones opcionales del rezo: Fátima después de cada decena, Salve y oración final en el
 * cierre. Las tres vienen activas; apagarlas cambia la lista y reinicia el rezo del día.
 */
@Composable
private fun SelectorOraciones(
    oraciones: OracionesRosario,
    onToggle: (OracionOpcional, Boolean) -> Unit,
    advertirReinicio: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rosario_opciones")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl)
                .padding(bottom = if (advertirReinicio) Spacing.xs else Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            OracionOpcional.entries.forEach { opcion ->
                val activa = opcion.incluidaEn(oraciones)
                PillRosario(
                    texto = if (activa) "✓ ${opcion.etiqueta}" else opcion.etiqueta,
                    activa = activa,
                    testTag = "rosario_opcion_${opcion.name.lowercase()}",
                    compact = true,
                    onClick = { onToggle(opcion, !activa) }
                )
            }
        }

        // El aviso solo aparece cuando hay un rezo empezado que se perdería al regenerar.
        if (advertirReinicio) {
            Text(
                text = "Cambiar las opciones reinicia el rezo de hoy.",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.sm)
            )
        }
    }
}

/** Píldora de los selectores del runner: rellena en oro cuando la opción está activa. */
@Composable
private fun PillRosario(
    texto: String,
    activa: Boolean,
    testTag: String,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(Radii.xxl)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (activa) GoldPrimary else Color.Transparent)
            .border(1.dp, if (activa) GoldPrimary else GoldSubtle, shape)
            .heightIn(min = ALTURA_MINIMA_PILDORA)
            .pressable(pressedAlpha = 0.7f, onClick = onClick)
            .padding(horizontal = if (compact) Spacing.md else Spacing.lg)
            .semantics { selected = activa }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            color = if (activa) NavyBackground else GoldPrimary,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Alto mínimo de las píldoras de los selectores, para un área táctil cómoda. */
private val ALTURA_MINIMA_PILDORA = 40.dp

/** Diámetro de cada cuenta de la decena, en paridad con la RN. */
private val TAMANO_CUENTA = 14.dp

/** Separación entre cuentas; el hilo asoma en estos tramos. */
private val SEPARACION_CUENTA = 6.dp

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
                translationX = (1f - entrada.value) * 12.dp.toPx()
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
            .padding(vertical = 12.dp)
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
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .drawBehind {
                    val diametro = TAMANO_CUENTA.toPx()
                    val y = size.height / 2f
                    val xInicio = diametro / 2f
                    val xFin = size.width - diametro / 2f
                    val grosor = 1.5.dp.toPx()

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
            horizontalArrangement = Arrangement.spacedBy(SEPARACION_CUENTA),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(CUENTAS_POR_DECENA) { indice ->
                val activa = indice < completadas
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

                Box(
                    modifier = Modifier
                        .size(TAMANO_CUENTA)
                        .clip(CircleShape)
                        .background(colorFondo)
                        .border(1.dp, colorBorde, CircleShape)
                        .semantics { selected = activa }
                        .testTag("rosario_cuenta")
                )
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
    autoavance: Autoavance,
    onAutoavance: (Autoavance) -> Unit,
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
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (pasoId != RosarioViewModel.PASO_COMPLETADO_ID) {
            FilaAutoavance(velocidad = autoavance, onSeleccion = onAutoavance)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    // La RN deja el botón al ancho del texto en el primer paso y lo estira a
                    // la mitad del footer cuando ya existe "← Anterior".
                    modifier = if (pasoIndex > 0) Modifier.weight(1f) else Modifier
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

/**
 * Selector de la velocidad del avance automático. "Off" deja el paso en manual; las otras
 * tres opciones lo pasan solo cuando termina el tiempo de rezo del texto.
 */
@Composable
private fun FilaAutoavance(
    velocidad: Autoavance,
    onSeleccion: (Autoavance) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 10.dp)
            .testTag("rosario_autoavance"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Avance",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Autoavance.entries.forEach { opcion ->
            PillRosario(
                texto = opcion.etiqueta,
                activa = opcion == velocidad,
                testTag = "rosario_auto_${opcion.name.lowercase()}",
                compact = true,
                onClick = { onSeleccion(opcion) }
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
            .padding(vertical = 16.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (principal) GoldLight else TextSecondary,
            fontSize = 16.sp,
            fontWeight = if (principal) FontWeight.Bold else FontWeight.SemiBold,
            letterSpacing = if (principal) 0.5.sp else 0.sp
        )
    }
}
