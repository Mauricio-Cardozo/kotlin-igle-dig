package com.example.ui.liturgia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.common.pressable
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurface
import com.example.ui.theme.Radii
import com.example.ui.theme.TextPrimary

@Composable
fun LiturgiaScreen(
    viewModel: LiturgiaViewModel,
    onNavigateToEvangelio: () -> Unit,
    onNavigateToRosario: () -> Unit,
    onNavigateToCoronilla: () -> Unit,
    onNavigateToSanto: (Long) -> Unit,
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
    onNavigateToAjustes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .testTag("screen_liturgia")
    ) {
        // 1. Encabezado fijo (como en la RN): no scrollea con el contenido y no
        // se bloquea con un spinner; fecha y saludo están listos desde el inicio.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                // 8 dp locales + 12 dp del modo inmersivo que aporta MainScreen
                // = los 20 dp de la RN.
                .padding(top = 8.dp)
        ) {
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

                Text(
                    text = "⚙️",
                    fontSize = 22.sp,
                    modifier = Modifier
                        .testTag("btn_ajustes")
                        .pressable(pressedAlpha = 0.7f, onClick = onNavigateToAjustes)
                        .padding(start = 12.dp, top = 4.dp)
                )
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
                        .padding(bottom = 20.dp)
                        .testTag("header_misa_title")
                )
            }
        }

        // 2. Contenido scrolleable: hero, rachas, santo del día y Propio del Tiempo.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // 2.1 Tarjeta Héroe (Evangelio)
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
                        .padding(bottom = 15.dp)
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
    ) {
        Text(
            text = label,
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
