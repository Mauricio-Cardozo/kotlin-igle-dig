package com.example.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtle
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Header de pantalla equivalente al `ScreenHeader` de la app RN:
 * fondo navyMid, borde inferior dorado, botón atrás circular de 36 dp,
 * superLabel 10/600/ls 2, título 18/700 y subtítulo 11 muted.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    superLabel: String? = null,
    subtitle: String? = null,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    rightSlot: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NavySurface)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = GoldSubtle,
                    start = Offset(0f, size.height - stroke / 2f),
                    end = Offset(size.width, size.height - stroke / 2f),
                    strokeWidth = stroke
                )
            }
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(NavySurfaceElevated, CircleShape)
                    .border(1.dp, GoldSubtle, CircleShape)
                    .pressable(pressedAlpha = 0.7f, onClick = onBack)
                    .testTag("btn_back"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "←",
                    color = GoldPrimary,
                    fontSize = 20.sp,
                    lineHeight = 22.sp
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            if (superLabel != null) {
                Text(
                    text = superLabel,
                    color = GoldPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
            }
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = if (superLabel != null) 2.dp else 0.dp)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        if (rightSlot != null) {
            rightSlot()
        }
    }
}
