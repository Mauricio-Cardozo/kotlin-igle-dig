package com.example.ui.liturgia

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.MisalSantosEntry
import com.example.data.db.Santo
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySeparator
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.MESES_ANIO
import com.example.util.hoy
import java.time.LocalDate

private val DIAS_SEMANA_CORTOS = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")

/**
 * Calendario Litúrgico (Fase 0.3). Grilla mensual (domingo primero) que marca los días con
 * lectura del Evangelio y muestra los santos y oraciones propias del día seleccionado.
 * Réplica de `app/(tabs)/calendario.tsx` del RN.
 */
@Composable
fun CalendarioScreen(
    viewModel: LiturgiaViewModel,
    onDiaClick: (String) -> Unit,
    onSantoClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.calendarioState.collectAsStateWithLifecycle()
    val hoyIso = hoy()

    LaunchedEffect(Unit) {
        viewModel.loadCalendario()
        viewModel.selectDiaCalendario(hoyIso)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .testTag("screen_calendario")
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Calendario Litúrgico",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Navegación de mes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MesNavButton("‹", "calendario_mes_anterior", onClick = viewModel::mesAnterior)
            Text(
                text = "${MESES_ANIO[state.mes - 1]} ${state.ano}",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag("calendario_mes_label")
            )
            MesNavButton("›", "calendario_mes_siguiente", onClick = viewModel::mesSiguiente)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Encabezado de días de la semana
        Row(modifier = Modifier.fillMaxWidth()) {
            DIAS_SEMANA_CORTOS.forEach { dia ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = dia, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Grilla de días
        val first = LocalDate.of(state.ano, state.mes, 1)
        val totalDias = first.lengthOfMonth()
        val primerDiaSemana = first.dayOfWeek.value % 7 // Domingo = 0
        val cells: List<Int?> = buildList {
            repeat(primerDiaSemana) { add(null) }
            for (d in 1..totalDias) add(d)
        }

        Column(modifier = Modifier.testTag("calendario_grid")) {
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { dia ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dia != null) {
                                DiaCelda(
                                    ano = state.ano,
                                    mes = state.mes,
                                    dia = dia,
                                    esHoy = fechaIso(state.ano, state.mes, dia) == hoyIso,
                                    seleccionado = fechaIso(state.ano, state.mes, dia) == state.selectedFecha,
                                    tieneLectura = state.lecturas.containsKey(fechaIso(state.ano, state.mes, dia)),
                                    tieneEvangelio = state.lecturas[fechaIso(state.ano, state.mes, dia)]?.tieneEvangelio == true,
                                    onClick = {
                                        val fecha = fechaIso(state.ano, state.mes, dia)
                                        viewModel.selectDiaCalendario(fecha)
                                        if (state.lecturas.containsKey(fecha)) onDiaClick(fecha)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "✦ Días con lectura del Evangelio",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )

        // Santos del día seleccionado
        Text(
            text = "SANTOS DEL DÍA",
            color = GoldPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = etiquetaDia(state.selectedFecha),
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (state.santosDelDia.isEmpty()) {
            Text(
                text = "No hay santos registrados para este día",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.testTag("calendario_sin_santos")
            )
        } else {
            Column(modifier = Modifier.testTag("calendario_santos")) {
                state.santosDelDia.forEach { santo ->
                    SantoListItem(santo = santo, onClick = { onSantoClick(santo.id) })
                }
            }
        }

        val propio = state.misalSantosDelDia.firstOrNull { !it.colecta.isNullOrBlank() }
            ?: state.misalSantosDelDia.firstOrNull()
        if (propio != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ORACIONES PROPIAS",
                color = GoldPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            MisalPropioDelDia(propio)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun fechaIso(ano: Int, mes: Int, dia: Int): String =
    "$ano-${mes.toString().padStart(2, '0')}-${dia.toString().padStart(2, '0')}"

private fun etiquetaDia(fechaIso: String?): String {
    if (fechaIso.isNullOrBlank()) return ""
    val parts = fechaIso.split("-")
    if (parts.size != 3) return fechaIso
    val d = parts[2].toIntOrNull() ?: return fechaIso
    val m = parts[1].toIntOrNull() ?: return fechaIso
    return "$d de ${MESES_ANIO.getOrElse(m - 1) { "" }} de ${parts[0]}"
}

@Composable
private fun MesNavButton(text: String, testTag: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(NavySurface)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = GoldPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DiaCelda(
    ano: Int,
    mes: Int,
    dia: Int,
    esHoy: Boolean,
    seleccionado: Boolean,
    tieneLectura: Boolean,
    tieneEvangelio: Boolean,
    onClick: () -> Unit
) {
    val background = when {
        esHoy -> GoldPrimary
        seleccionado -> GoldPrimary.copy(alpha = 0.2f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .then(if (tieneLectura) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag("calendario_dia_${fechaIso(ano, mes, dia)}"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$dia",
                color = when {
                    esHoy -> NavyBackground
                    tieneLectura -> TextPrimary
                    else -> TextSecondary.copy(alpha = 0.4f)
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (tieneEvangelio) {
                Text(text = "✦", color = GoldPrimary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SantoListItem(santo: Santo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NavySurface)
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("calendario_santo_${santo.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (santo.titulo.isNullOrBlank()) santo.nombre else "${santo.nombre}, ${santo.titulo}",
            color = GoldLight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
        Text(text = "›", color = GoldPrimary, fontSize = 20.sp)
    }
}

@Composable
private fun MisalPropioDelDia(entry: MisalSantosEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NavySeparator, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(NavySurfaceElevated)
            .padding(16.dp)
    ) {
        if (!entry.antifonaEntrada.isNullOrBlank()) {
            PrayerCard(label = "Antífona de entrada", text = entry.antifonaEntrada)
        }
        if (!entry.colecta.isNullOrBlank()) {
            PrayerCard(label = "Oración colecta", text = entry.colecta)
        }
        if (!entry.oracionOfrendas.isNullOrBlank()) {
            PrayerCard(label = "Oración sobre las ofrendas", text = entry.oracionOfrendas)
        }
        if (!entry.prefacio.isNullOrBlank()) {
            PrayerCard(label = "Prefacio", text = entry.prefacio)
        }
        if (!entry.antifonaComunion.isNullOrBlank()) {
            PrayerCard(label = "Antífona de comunión", text = entry.antifonaComunion)
        }
        if (!entry.postcomunion.isNullOrBlank()) {
            PrayerCard(label = "Postcomunión", text = entry.postcomunion)
        }
    }
}
