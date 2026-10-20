package com.example.ui.oracion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.common.FontSizeControl
import com.example.ui.common.pressable
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtle
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.Radii
import com.example.ui.theme.Spacing
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Hoja inferior con los ajustes del rezo: idioma, misterios, oraciones opcionales, velocidad
 * del avance y tamaño de texto. Antes estos controles vivían en tres filas sobre el texto y
 * en la cabecera; sacarlos a una hoja deja la oración casi a pantalla completa, como Rosarium.
 *
 * Las etiquetas de la interfaz siguen en español; el idioma solo cambia los textos del rezo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesRosarioSheet(
    idioma: IdiomaRosario,
    misterioManual: MisterioRosario,
    oraciones: OracionesRosario,
    autoavance: Autoavance,
    fontMultiplier: Float,
    advertirReinicio: Boolean,
    onIdioma: (IdiomaRosario) -> Unit,
    onMisterio: (MisterioRosario) -> Unit,
    onOracion: (OracionOpcional, Boolean) -> Unit,
    onAutoavance: (Autoavance) -> Unit,
    onDecreaseFont: () -> Unit,
    onIncreaseFont: () -> Unit,
    onResetFont: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NavySurfaceElevated,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.xxl)
                .testTag("rosario_ajustes_sheet")
        ) {
            Text(
                text = "Ajustes del rosario",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(Spacing.sm))

            SeccionAjustes("Idioma") {
                FilaPildoras {
                    IdiomaRosario.entries.forEach { opcion ->
                        PillRosario(
                            texto = opcion.etiqueta,
                            activa = opcion == idioma,
                            testTag = "rosario_idioma_${opcion.name.lowercase()}",
                            onClick = { onIdioma(opcion) }
                        )
                    }
                }
            }

            SeccionAjustes("Misterios") {
                FilaPildoras {
                    MisterioRosario.entries.forEach { opcion ->
                        PillRosario(
                            texto = opcion.etiqueta,
                            activa = opcion == misterioManual,
                            testTag = "rosario_misterio_${opcion.name.lowercase()}",
                            onClick = { onMisterio(opcion) }
                        )
                    }
                }
            }

            SeccionAjustes("Oraciones") {
                FilaPildoras {
                    OracionOpcional.entries.forEach { opcion ->
                        val activa = opcion.incluidaEn(oraciones)
                        PillRosario(
                            texto = if (activa) "✓ ${opcion.etiqueta}" else opcion.etiqueta,
                            activa = activa,
                            testTag = "rosario_opcion_${opcion.name.lowercase()}",
                            compact = true,
                            onClick = { onOracion(opcion, !activa) }
                        )
                    }
                }
                if (advertirReinicio) {
                    Text(
                        text = "Cambiar las opciones reinicia el rezo de hoy.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = Spacing.sm)
                    )
                }
            }

            SeccionAjustes("Avance") {
                FilaPildoras {
                    Autoavance.entries.forEach { opcion ->
                        PillRosario(
                            texto = opcion.etiqueta,
                            activa = opcion == autoavance,
                            testTag = "rosario_auto_${opcion.name.lowercase()}",
                            compact = true,
                            onClick = { onAutoavance(opcion) }
                        )
                    }
                }
            }

            SeccionAjustes("Tamaño de texto") {
                FontSizeControl(
                    multiplier = fontMultiplier,
                    onDecrease = onDecreaseFont,
                    onIncrease = onIncreaseFont,
                    onReset = onResetFont
                )
            }

            Spacer(Modifier.height(Spacing.xl))
            BotonListo(onClick = onDismiss)
        }
    }
}

/** Título de sección de la hoja, al estilo de la sección INFORMACIÓN de Ajustes. */
@Composable
private fun SeccionAjustes(titulo: String, contenido: @Composable () -> Unit) {
    Text(
        text = titulo.uppercase(),
        color = GoldLight,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.sm)
    )
    contenido()
}

/** Fila de píldoras desplazable cuando no caben todas en pantalla. */
@Composable
private fun FilaPildoras(contenido: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        contenido()
    }
}

/** Alto mínimo de las píldoras, para un área táctil cómoda. */
private val ALTURA_MINIMA_PILDORA = 40.dp

/** Píldora de los ajustes: rellena en oro cuando la opción está activa. */
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
            .background(if (activa) GoldPrimary else androidx.compose.ui.graphics.Color.Transparent)
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

/** Cierra la hoja; el gesto de arrastre y el botón atrás también la descartan. */
@Composable
private fun BotonListo(onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radii.lg)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(GoldSubtle)
            .border(1.dp, GoldPrimary, shape)
            .pressable(pressedAlpha = 0.8f, onClick = onClick)
            .padding(vertical = Spacing.lg)
            .testTag("rosario_ajustes_listo"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Listo",
            color = GoldLight,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
