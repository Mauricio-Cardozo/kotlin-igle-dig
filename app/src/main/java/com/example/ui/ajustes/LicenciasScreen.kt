package com.example.ui.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.common.ScreenHeader
import com.example.ui.theme.GoldLight
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.TextSecondary

/**
 * Licencias y créditos de la app. Rosarium (Krixec) se distribuye con licencia MIT+Credo, que
 * exige incluir el aviso de copyright, el permiso y el Credo latino en las copias; el texto
 * completo se muestra aquí y vive también en `THIRD_PARTY_LICENSES/Rosarium-LICENSE.txt`.
 * La licencia cubre el código, no los textos litúrgicos ni sus traducciones.
 */
@Composable
fun LicenciasScreen(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .testTag("screen_licencias")
    ) {
        ScreenHeader(
            title = "Licencias",
            subtitle = "Créditos y avisos legales",
            showBack = true,
            onBack = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Rosarium (Krixec) — MIT+Credo",
                color = GoldLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Parte del rosario de esta app adapta reglas de Rosarium, reescritas en " +
                    "Kotlin. Su licencia exige mostrar este aviso completo:",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
            )
            Text(
                text = LicenciasTextos.ROSARIUM,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.testTag("licencias_texto")
            )
            Text(
                text = "La licencia cubre el código, no los textos litúrgicos ni sus " +
                    "traducciones, que esta app no redistribuye.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
