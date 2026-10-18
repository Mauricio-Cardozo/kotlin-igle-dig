package com.example.ui.liturgia

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.Lectura
import com.example.data.db.DiaLectura
import com.example.data.db.MisalPropioEntry
import com.example.data.db.MisalSantosEntry
import com.example.data.db.Santo
import com.example.data.repository.LiturgiaRepository
import com.example.data.user.StreaksPreferencesRepository
import com.example.ui.theme.GoldPrimary
import com.example.util.DateProvider
import com.example.util.DefaultDateProvider
import com.example.util.SEASON_COLOR
import com.example.util.SEASON_COLOR_NAME
import com.example.util.SEASON_EMOJI
import com.example.util.detectSeason
import com.example.util.fechaActualLarga
import com.example.util.hoy
import com.example.util.parseWeekNumber
import com.example.util.saludo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SeasonDisplayInfo(
    val season: String,
    val label: String,
    val color: Color,
    val emoji: String,
    val colorName: String
)

data class LiturgiaUiState(
    val isLoading: Boolean = true,
    val fechaLarga: String = "",
    val misaTitle: String? = null,
    val saludo: String = "Bendecido día",
    val season: String? = null,
    val seasonInfo: SeasonDisplayInfo? = null,
    val seasonLabel: String? = null,
    val gospelQuote: String? = null,
    val gospelRef: String? = null,
    val rachaRosario: Int = 0,
    val totalRosario: Int = 0,
    val rachaCoronilla: Int = 0,
    val totalCoronilla: Int = 0,
    val santoDelDia: Santo? = null,
    val propio: MisalPropioEntry? = null,
    val errorMessage: String? = null
)

/** Estado de la pantalla "Evangelio del Día". */
data class EvangelioUiState(
    val isLoading: Boolean = true,
    val fecha: String = "",
    val lectura: Lectura? = null,
    val errorMessage: String? = null
)

/** Estado de la pantalla "Santo del Día" (detalle). */
data class SantoDetalleUiState(
    val isLoading: Boolean = true,
    val santo: Santo? = null,
    val misalSantos: List<MisalSantosEntry> = emptyList()
)

/** Estado de la pantalla "Calendario Litúrgico". */
data class CalendarioUiState(
    val ano: Int,
    val mes: Int,
    val isLoading: Boolean = true,
    val lecturas: Map<String, DiaLectura> = emptyMap(),
    val selectedFecha: String? = null,
    val santosDelDia: List<Santo> = emptyList(),
    val misalSantosDelDia: List<MisalSantosEntry> = emptyList()
)

class LiturgiaViewModel(
    private val liturgiaRepository: LiturgiaRepository,
    private val streaksRepository: StreaksPreferencesRepository,
    private val dateProvider: DateProvider = DefaultDateProvider()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LiturgiaUiState(
            // El encabezado del Home se pinta al instante (como la RN, que
            // calcula fecha y saludo al renderizar); el resto llega async.
            fechaLarga = fechaActualLarga(dateProvider.today()),
            saludo = saludo(dateProvider.nowTime().hour)
        )
    )
    val uiState: StateFlow<LiturgiaUiState> = _uiState.asStateFlow()

    private val _evangelioState = MutableStateFlow(EvangelioUiState())
    val evangelioState: StateFlow<EvangelioUiState> = _evangelioState.asStateFlow()

    private val _santoDetalleState = MutableStateFlow(SantoDetalleUiState())
    val santoDetalleState: StateFlow<SantoDetalleUiState> = _santoDetalleState.asStateFlow()

    private val _calendarioState = MutableStateFlow(
        CalendarioUiState(
            ano = dateProvider.today().year,
            mes = dateProvider.today().monthValue
        )
    )
    val calendarioState: StateFlow<CalendarioUiState> = _calendarioState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val today = dateProvider.today()
                val todayIso = hoy(today)
                val hour = dateProvider.nowTime().hour
                val headerDate = fechaActualLarga(today)
                val greeting = saludo(hour)

                val streakStats = streaksRepository.streakStatsFlow.first()

                val lectura = liturgiaRepository.getLecturaDelDia(todayIso)
                val temporadas = liturgiaRepository.getMisalTemporadas()

                val santos = liturgiaRepository.getSantosDelDia(today.monthValue, today.dayOfMonth)
                val santoDelDia = santos.firstOrNull()

                var propioEntry: MisalPropioEntry? = null
                val misaTitle = lectura?.tituloMisa
                val detectedSeason = misaTitle?.let { detectSeason(it) }

                if (misaTitle != null) {
                    if (detectedSeason != null) {
                        val week = parseWeekNumber(misaTitle)
                        propioEntry = liturgiaRepository.getMisalPropioPorSemana(detectedSeason, week)
                    } else {
                        val fallback = liturgiaRepository.getSeasonFromNearestLectura(todayIso)
                        if (fallback != null) {
                            propioEntry = liturgiaRepository.getMisalPropioPorSemana(fallback.season, fallback.week)
                        }
                    }
                }

                val seasonData = detectedSeason?.let { s -> temporadas.find { it.temporada == s } }
                val seasonInfo = if (detectedSeason != null && seasonData != null) {
                    val hexColor = SEASON_COLOR[detectedSeason]
                    val color = if (hexColor != null) {
                        Color(android.graphics.Color.parseColor(hexColor))
                    } else {
                        GoldPrimary
                    }
                    SeasonDisplayInfo(
                        season = detectedSeason,
                        label = seasonData.temporadaLabel,
                        color = color,
                        emoji = SEASON_EMOJI[detectedSeason] ?: "🌿",
                        colorName = SEASON_COLOR_NAME[detectedSeason] ?: ""
                    )
                } else null

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        fechaLarga = headerDate,
                        misaTitle = misaTitle,
                        saludo = greeting,
                        season = detectedSeason,
                        seasonInfo = seasonInfo,
                        seasonLabel = seasonData?.temporadaLabel,
                        gospelQuote = lectura?.evangelio,
                        gospelRef = lectura?.evangelioRef,
                        rachaRosario = streakStats.rachaRosarioDias,
                        totalRosario = streakStats.rosarioTotal,
                        rachaCoronilla = streakStats.rachaCoronillaDias,
                        totalCoronilla = streakStats.coronillaTotal,
                        santoDelDia = santoDelDia,
                        propio = propioEntry
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error al cargar la información litúrgica"
                    )
                }
            }
        }
    }

    /**
     * Carga la lectura (Evangelio) para una fecha ISO (YYYY-MM-DD). Si [fecha] es null usa hoy.
     */
    fun loadEvangelio(fecha: String? = null) {
        viewModelScope.launch {
            val target = fecha ?: hoy(dateProvider.today())
            _evangelioState.update { it.copy(isLoading = true, fecha = target, errorMessage = null) }
            try {
                val lectura = liturgiaRepository.getLecturaDelDia(target)
                _evangelioState.update { it.copy(isLoading = false, lectura = lectura) }
            } catch (e: Exception) {
                _evangelioState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Error al cargar el Evangelio")
                }
            }
        }
    }

    /**
     * Carga el santo por id junto con su Propio (misal_santos) del día.
     */
    fun loadSantoDetalle(id: Long) {
        viewModelScope.launch {
            _santoDetalleState.update { it.copy(isLoading = true) }
            try {
                val santo = liturgiaRepository.getSantoById(id)
                val misal = if (santo != null) {
                    liturgiaRepository.getMisalSantosDelDia(santo.mes, santo.dia)
                } else {
                    emptyList()
                }
                _santoDetalleState.update {
                    it.copy(isLoading = false, santo = santo, misalSantos = misal)
                }
            } catch (e: Exception) {
                _santoDetalleState.update { it.copy(isLoading = false, santo = null) }
            }
        }
    }

    /**
     * Carga el mes indicado en el calendario. Si se omite, recarga el mes actual en estado.
     */
    fun loadCalendario(ano: Int = _calendarioState.value.ano, mes: Int = _calendarioState.value.mes) {
        viewModelScope.launch {
            _calendarioState.update { it.copy(ano = ano, mes = mes, isLoading = true) }
            try {
                val lecturas = liturgiaRepository.getLecturasDelMes(ano, mes)
                _calendarioState.update {
                    it.copy(isLoading = false, lecturas = lecturas.associateBy { d -> d.fecha })
                }
            } catch (e: Exception) {
                _calendarioState.update { it.copy(isLoading = false, lecturas = emptyMap()) }
            }
        }
    }

    fun mesAnterior() {
        val current = _calendarioState.value
        val (ano, mes) = if (current.mes == 1) current.ano - 1 to 12 else current.ano to current.mes - 1
        loadCalendario(ano, mes)
    }

    fun mesSiguiente() {
        val current = _calendarioState.value
        val (ano, mes) = if (current.mes == 12) current.ano + 1 to 1 else current.ano to current.mes + 1
        loadCalendario(ano, mes)
    }

    /**
     * Selecciona un día del calendario y carga los santos y el Propio de esa fecha.
     */
    fun selectDiaCalendario(fecha: String) {
        viewModelScope.launch {
            _calendarioState.update { it.copy(selectedFecha = fecha) }
            try {
                val parts = fecha.split("-")
                val mes = parts[1].toInt()
                val dia = parts[2].toInt()
                val santos = liturgiaRepository.getSantosDelDia(mes, dia)
                val misal = liturgiaRepository.getMisalSantosDelDia(mes, dia)
                _calendarioState.update {
                    it.copy(santosDelDia = santos, misalSantosDelDia = misal)
                }
            } catch (e: Exception) {
                _calendarioState.update { it.copy(santosDelDia = emptyList(), misalSantosDelDia = emptyList()) }
            }
        }
    }
}

class LiturgiaViewModelFactory(
    private val liturgiaRepository: LiturgiaRepository,
    private val streaksRepository: StreaksPreferencesRepository,
    private val dateProvider: DateProvider = DefaultDateProvider()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LiturgiaViewModel::class.java)) {
            return LiturgiaViewModel(liturgiaRepository, streaksRepository, dateProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
    }
}
