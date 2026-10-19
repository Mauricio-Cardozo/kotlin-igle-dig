package com.example.ui.oracion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.common.SectionCard
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.Spacing
import com.example.ui.theme.TextPrimary

/**
 * Sección del hub de Oración; espejo de `ORACIONES_SECCIONES` de la RN
 * (`app/(tabs)/oracion.tsx`). La [ruta] es la de navegación en esta app.
 */
data class SeccionOracion(
    val id: String,
    val icono: String,
    val titulo: String,
    val subtitulo: String,
    val ruta: String
)

/** Orden, textos, íconos y rutas idénticos a la RN. */
internal val SECCIONES_ORACION = listOf(
    SeccionOracion("rosario", "📿", "Santo Rosario", "Guía completa con misterios del día", "rosario/guia"),
    SeccionOracion("coronilla", "🌿", "Coronilla", "Divina Misericordia", "rosario/coronilla"),
    SeccionOracion("favoritos", "❤️", "Favoritos", "Versículos, numerales y lecturas guardadas", "favoritos"),
    SeccionOracion("oraciones", "📖", "Oraciones", "Del Vaticano: Padre nuestro, Ave María, Credo y más", "oraciones"),
    SeccionOracion("jaculatorias", "🔥", "Jaculatorias", "Oraciones breves para el día", "oraciones/jaculatorias"),
    SeccionOracion("novenas", "🕯️", "Novenas", "18 devociones de 9 días", "oraciones/novena")
)

/**
 * Hub del tab Oración (espejo de `app/(tabs)/oracion.tsx`): encabezado de marca
 * y las tarjetas de cada devoción.
 */
@Composable
fun OracionHubScreen(
    onSeccionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .verticalScroll(rememberScrollState())
            .testTag("screen_oracion")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl)
                // 4 dp locales + 12 dp del modo inmersivo que aporta MainScreen
                // = los 16 dp del hub de la RN.
                .padding(top = Spacing.xs)
                .padding(bottom = Spacing.xxl)
        ) {
            Text(
                text = "✝ IGLESIA DIGITAL",
                color = GoldPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .padding(bottom = Spacing.xs)
                    .testTag("header_brand")
            )
            Text(
                text = "Oración",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("header_title")
            )
        }

        SECCIONES_ORACION.forEach { seccion ->
            SectionCard(
                icono = seccion.icono,
                titulo = seccion.titulo,
                subtitulo = seccion.subtitulo,
                onClick = { onSeccionClick(seccion.ruta) },
                modifier = Modifier.testTag("oracion_card_${seccion.id}")
            )
        }
    }
}
