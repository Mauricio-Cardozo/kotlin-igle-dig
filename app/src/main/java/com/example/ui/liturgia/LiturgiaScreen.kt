package com.example.ui.liturgia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurface
import com.example.ui.theme.Radii
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LiturgiaScreen(
    viewModel: LiturgiaViewModel,
    onNavigateToEvangelio: () -> Unit,
    onNavigateToRosario: () -> Unit,
    onNavigateToCoronilla: () -> Unit,
    onNavigateToSanto: (Long) -> Unit,
    onNavigateToMisalPropio: () -> Unit,
    onNavigateToAjustes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LiturgiaContent(
        uiState = uiState,
        onNavigateToEvangelio = onNavigateToEvangelio,
        onNavigateToRosario = onNavigateToRosario,
        onNavigateToCoronilla = onNavigateToCoronilla,
        onNavigateToSanto = onNavigateToSanto,
        onNavigateToMisalPropio = onNavigateToMisalPropio,
        onNavigateToAjustes = onNavigateToAjustes,
        modifier = modifier
    )
}

@Composable
fun LiturgiaContent(
    uiState: LiturgiaUiState,
    onNavigateToEvangelio: () -> Unit,
    onNavigateToRosario: () -> Unit,
    onNavigateToCoronilla: () -> Unit,
    onNavigateToSanto: (Long) -> Unit,
    onNavigateToMisalPropio: () -> Unit,
    onNavigateToAjustes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .testTag("screen_liturgia")
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = GoldPrimary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("liturgia_loading")
            )
        } else {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // 1. Encabezado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "✝ IGLESIA DIGITAL",
                            color = GoldPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .testTag("header_brand")
                        )
                        Text(
                            text = "Liturgia",
                            color = TextPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .testTag("header_title")
                        )
                    }

                    IconButton(
                        onClick = onNavigateToAjustes,
                        modifier = Modifier.testTag("btn_ajustes")
                    ) {
                        Text(
                            text = "⚙️",
                            fontSize = 22.sp
                        )
                    }
                }

                Text(
                    text = uiState.fechaLarga,
                    color = GoldPrimary,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .testTag("header_date")
                )

                if (!uiState.misaTitle.isNullOrBlank()) {
                    Text(
                        text = uiState.misaTitle,
                        color = GoldLight,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .testTag("header_misa_title")
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 2. Tarjeta Héroe (Evangelio)
                HeroSection(
                    greeting = uiState.saludo,
                    season = uiState.seasonInfo,
                    gospelQuote = uiState.gospelQuote,
                    gospelRef = uiState.gospelRef,
                    onPress = onNavigateToEvangelio,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(15.dp))

                // 3. Dos tarjetas de racha (Rosario y Coronilla)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StreakCard(
                        label = "ROSARIO",
                        count = uiState.totalRosario,
                        streakDays = uiState.rachaRosario,
                        onPress = onNavigateToRosario,
                        modifier = Modifier.weight(1f)
                    )
                    StreakCard(
                        label = "CORONILLA",
                        count = uiState.totalCoronilla,
                        streakDays = uiState.rachaCoronilla,
                        onPress = onNavigateToCoronilla,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                // 4. Tarjeta SANTO DEL DÍA
                if (uiState.santoDelDia != null) {
                    val santo = uiState.santoDelDia
                    SantoCard(
                        nombre = santo.nombre,
                        titulo = santo.titulo,
                        biografia = santo.biografia,
                        onPress = { onNavigateToSanto(santo.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(15.dp))
                }

                // 5. PROPIO DEL TIEMPO
                val propio = uiState.propio
                if (propio != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("section_propio")
                    ) {
                        Text(
                            text = "PROPIO DEL TIEMPO",
                            color = GoldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Text(
                            text = "${uiState.seasonLabel ?: ""} — ${propio.dia}",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (!propio.antifonaEntrada.isNullOrBlank()) {
                            PrayerCard(label = "Antífona de entrada", text = propio.antifonaEntrada)
                        }
                        if (!propio.colecta.isNullOrBlank()) {
                            PrayerCard(label = "Oración colecta", text = propio.colecta)
                        }
                        if (!propio.oracionOfrendas.isNullOrBlank()) {
                            PrayerCard(label = "Oración sobre las ofrendas", text = propio.oracionOfrendas)
                        }
                        if (!propio.postcomunion.isNullOrBlank()) {
                            PrayerCard(label = "Postcomunión", text = propio.postcomunion)
                        }
                        if (!propio.antifonaComunion.isNullOrBlank()) {
                            PrayerCard(label = "Antífona de comunión", text = propio.antifonaComunion)
                        }
                    }
                } else if (uiState.season != null) {
                    // Si no hay propio pero sí temporada, tarjeta hacia /misal/propio
                    val shape = RoundedCornerShape(Radii.lg)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(NavySurface)
                            .clickable(onClick = onNavigateToMisalPropio)
                            .padding(18.dp)
                            .testTag("propio_fallback_card")
                    ) {
                        Text(
                            text = "PROPIO DEL TIEMPO",
                            color = GoldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ver oraciones del Propio del Tiempo",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "›",
                                color = GoldPrimary,
                                fontSize = 22.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun PrayerCard(label: String, text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Radii.lg)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NavySurface)
            .padding(16.dp)
            .padding(bottom = 10.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = GoldPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
}
