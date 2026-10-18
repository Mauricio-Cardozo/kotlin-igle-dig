package com.example.ui.liturgia

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.common.pressable
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.Radii
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SantoCard(
    nombre: String,
    titulo: String?,
    biografia: String,
    onPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(Radii.lg)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NavySurface)
            .pressable(pressedAlpha = 0.7f, onClick = onPress)
            .padding(20.dp)
            .testTag("santo_card")
    ) {
        Text(
            text = "✝",
            color = TextPrimary,
            fontSize = 28.sp,
            modifier = Modifier.padding(end = 14.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SANTO DEL DÍA",
                color = GoldPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = nombre,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (!titulo.isNullOrBlank()) {
                Text(
                    text = titulo,
                    color = GoldLight,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (biografia.isNotBlank()) {
                Text(
                    text = biografia,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Text(
            text = "›",
            color = GoldPrimary,
            fontSize = 24.sp,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
