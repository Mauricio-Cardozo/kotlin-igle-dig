package com.example.ui.liturgia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.common.pressable
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.Radii
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HeroSection(
    greeting: String,
    season: SeasonDisplayInfo?,
    gospelQuote: String?,
    gospelRef: String?,
    onPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accentColor = season?.color ?: GoldPrimary
    val shape = RoundedCornerShape(Radii.lg)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(NavySurface, NavySurfaceElevated)
                )
            )
            .drawBehind {
                // Left accent border (4.dp width)
                val strokeWidth = 4.dp.toPx()
                drawLine(
                    color = accentColor,
                    start = Offset(strokeWidth / 2, 0f),
                    end = Offset(strokeWidth / 2, size.height),
                    strokeWidth = strokeWidth
                )
            }
            .pressable(pressedAlpha = 0.9f, onClick = onPress)
            .padding(20.dp)
            .testTag("hero_section")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (season != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(season.color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${season.emoji} ${season.label} · ${season.colorName}",
                        color = GoldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = greeting,
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            val quoteText = gospelQuote?.trim() ?: "Yo soy el camino, la verdad y la vida."
            Text(
                text = "\u201C$quoteText\u201D",
                color = TextPrimary,
                fontSize = 19.sp,
                fontStyle = FontStyle.Italic,
                lineHeight = 28.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            if (!gospelRef.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = gospelRef,
                    color = TextSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
