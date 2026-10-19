package com.example.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NavySurface
import com.example.ui.theme.Radii
import com.example.ui.theme.Spacing
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Tarjeta de sección de los hubs, espejo del `SectionCard` de la RN
 * (`components/section-card.tsx`): emoji, título, subtítulo y chevrón ›,
 * con fondo `navyMid` (#1A2D45) y radio de 16 dp.
 */
@Composable
fun SectionCard(
    icono: String,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Márgenes de la RN: 20 dp laterales y 12 dp entre tarjetas.
            .padding(horizontal = Spacing.xl)
            .padding(bottom = Spacing.md)
            .clip(RoundedCornerShape(Radii.xl))
            .background(NavySurface)
            .pressable(pressedAlpha = 0.7f, onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icono,
            fontSize = 32.sp,
            modifier = Modifier.padding(end = Spacing.lg)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitulo,
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Text(
            text = "›",
            color = TextPrimary,
            fontSize = 24.sp,
            modifier = Modifier.padding(start = Spacing.sm)
        )
    }
}
