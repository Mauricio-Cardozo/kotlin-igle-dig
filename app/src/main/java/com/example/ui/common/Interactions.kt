package com.example.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

/**
 * Feedback de presión al estilo de la app RN: baja la opacidad en vez de
 * pintar el ripple de Material 3 (que la referencia no usa).
 */
@Composable
fun Modifier.pressable(
    pressedAlpha: Float = 0.7f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    return this
        .alpha(if (pressed && enabled) pressedAlpha else 1f)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}
