package com.example.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ErrorColor
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.Radii
import com.example.ui.theme.Spacing
import com.example.ui.theme.TextSecondary

/**
 * Estados compartidos de carga/vacío/error, en el estilo de la RN (decisión 3
 * de #15): texto plano, sin íconos ni componentes Material con ripple. El
 * press feedback del botón «Reintentar» es la opacidad 0.7 de la RN.
 */

/** Spinner simple + texto atenuado centrados, como el `ActivityIndicator` de la RN. */
@Composable
fun LoadingState(
    message: String = "Cargando…",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("loading_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.padding(horizontal = Spacing.xl)
        ) {
            CircularProgressIndicator(color = GoldPrimary)
            if (message.isNotBlank()) {
                Text(
                    text = message,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Texto atenuado centrado, como el `emptyText` de la RN. */
@Composable
fun EmptyState(
    message: String = "Sin resultados.",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.huge)
        )
    }
}

/** Mensaje `#E07070` centrado y, si hay reintento, el botón dorado de la RN. */
@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xl)
            .testTag("error_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = ErrorColor,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(Spacing.lg))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radii.lg))
                    .background(GoldPrimary)
                    .pressable(pressedAlpha = 0.7f, onClick = onRetry)
                    .padding(horizontal = Spacing.xxl, vertical = 10.dp)
                    .testTag("retry_button"),
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
}
