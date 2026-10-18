package com.example.ui.liturgia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.Lectura
import com.example.ui.common.EmptyState
import com.example.ui.common.LoadingState
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.formatoFecha

/**
 * Pantalla "Evangelio del Día": muestra las lecturas litúrgicas de una fecha
 * (o de hoy si [fecha] es null). Reemplaza al ComingSoonScreen (Fase 0.3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvangelioScreen(
    viewModel: LiturgiaViewModel,
    fecha: String?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(fecha) {
        viewModel.loadEvangelio(fecha)
    }

    val state by viewModel.evangelioState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Evangelio del Día",
                            style = MaterialTheme.typography.titleLarge,
                            color = GoldLight,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.fecha.isNotBlank()) {
                            Text(
                                text = runCatching { formatoFecha(state.fecha) }.getOrDefault(state.fecha),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("evangelio_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = GoldLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyBackground,
                    titleContentColor = GoldLight
                )
            )
        },
        containerColor = NavyBackground,
        modifier = modifier.testTag("screen_evangelio")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val lectura = state.lectura
            when {
                state.isLoading -> LoadingState(message = "Cargando la liturgia de la Palabra...")
                lectura == null -> EmptyState(
                    title = "Sin lecturas",
                    subtitle = "No hay lecturas registradas para esta fecha."
                )
                else -> EvangelioContent(lectura)
            }
        }
    }
}

@Composable
private fun EvangelioContent(lectura: Lectura) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("evangelio_lazy_column")
    ) {
        if (!lectura.tituloMisa.isNullOrBlank()) {
            item {
                Text(
                    text = lectura.tituloMisa,
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldLight,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("evangelio_titulo_misa")
                )
            }
        }

        if (!lectura.primeraLectura.isNullOrBlank()) {
            item {
                ReadingBlock("Primera Lectura", lectura.primeraLecturaRef, lectura.primeraLectura)
            }
        }
        if (!lectura.salmo.isNullOrBlank()) {
            item { ReadingBlock("Salmo Responsorial", null, lectura.salmo) }
        }
        if (!lectura.segundaLectura.isNullOrBlank()) {
            item {
                ReadingBlock("Segunda Lectura", lectura.segundaLecturaRef, lectura.segundaLectura)
            }
        }
        if (!lectura.aleluia.isNullOrBlank()) {
            item { ReadingBlock("Aleluia", null, lectura.aleluia) }
        }
        if (!lectura.evangelio.isNullOrBlank()) {
            item {
                ReadingBlock(
                    label = "Evangelio",
                    ref = lectura.evangelioRef,
                    text = lectura.evangelio,
                    emphasized = true
                )
            }
        }
        if (!lectura.comentarioPapal.isNullOrBlank()) {
            item { ReadingBlock("Comentario", null, lectura.comentarioPapal) }
        }
        if (!lectura.url.isNullOrBlank()) {
            item {
                Text(
                    text = "Fuente: ${lectura.url}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun ReadingBlock(
    label: String,
    ref: String?,
    text: String,
    emphasized: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NavySurface)
            .padding(16.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = GoldPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (!ref.isNullOrBlank()) {
            Text(
                text = ref,
                color = GoldLight,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 15.sp,
            lineHeight = 24.sp
        )
    }
}
