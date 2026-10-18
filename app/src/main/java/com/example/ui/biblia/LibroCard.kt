package com.example.ui.biblia

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.common.pressScale
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavySeparator
import com.example.ui.theme.NavySurface
import com.example.ui.theme.Radii
import com.example.ui.theme.TextSecondary

/**
 * Tarjeta de libro de la grilla de la Biblia, equivalente al `LibroCard` de la
 * RN: abreviatura grande, nombre debajo, badge † si es deuterocanónico y
 * feedback de presión con escala 0.95 (sin ícono ni chevron).
 */
@Composable
fun LibroCard(
    nombre: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    deuterocanonico: Boolean = false
) {
    val shape = RoundedCornerShape(Radii.lg)
    Box(
        modifier = modifier
            .height(90.dp)
            .clip(shape)
            .background(NavySurface)
            .border(1.dp, NavySeparator, shape)
            .pressScale(pressedScale = 0.95f, onClick = onClick)
            .testTag("book_item_$nombre"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = abreviatura(nombre),
                color = GoldPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = nombre,
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (deuterocanonico) {
            Text(
                text = "†",
                color = GoldPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 10.dp)
            )
        }
    }
}
