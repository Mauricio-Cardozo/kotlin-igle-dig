package com.example.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.user.UserPreferencesRepository
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtle
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.Radii
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

/**
 * Control de tamaño de fuente A− | % | A+, espejo del `FontSizeControl` de la RN
 * (`components/font-size-control.tsx`). El porcentaje central restablece el tamaño.
 */
@Composable
fun FontSizeControl(
    multiplier: Float,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FontSizeButton(
            label = "A−",
            enabled = puedeDisminuir(multiplier),
            onClick = onDecrease,
            testTag = "font_decrease_button"
        )
        FontSizeValueButton(
            multiplier = multiplier,
            onClick = onReset,
            testTag = "font_reset_button"
        )
        FontSizeButton(
            label = "A+",
            enabled = puedeAumentar(multiplier),
            onClick = onIncrease,
            testTag = "font_increase_button"
        )
    }
}

/** Botón cuadrado de 30 dp del control de tamaño (equivalente al `btn` de la RN). */
@Composable
fun FontSizeButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val shape = RoundedCornerShape(Radii.md)
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(shape)
            .background(NavySurfaceElevated)
            .border(1.dp, GoldSubtle, shape)
            .pressable(pressedAlpha = 0.7f, enabled = enabled, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) GoldPrimary else TextSecondary.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Indicador de porcentaje que restablece el tamaño al tocarse (equivalente al `valBtn` de la RN). */
@Composable
fun FontSizeValueButton(
    multiplier: Float,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .height(24.dp)
            .clip(RoundedCornerShape(Radii.sm))
            .background(NavySurfaceElevated)
            .pressable(pressedAlpha = 0.7f, onClick = onClick)
            .padding(horizontal = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${(multiplier * 100).roundToInt()}%",
            color = GoldLight,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Los extremos (0.8 / 1.5) no pueden seguir alejándose del rango. */
private fun puedeDisminuir(multiplier: Float): Boolean =
    UserPreferencesRepository.decrement(multiplier) != multiplier

private fun puedeAumentar(multiplier: Float): Boolean =
    UserPreferencesRepository.increment(multiplier) != multiplier
