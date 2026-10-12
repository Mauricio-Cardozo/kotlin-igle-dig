package com.example.ui.liturgia

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.Lectura
import com.example.data.db.MisalPropioEntry
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

class LiturgiaViewModel(
    private val liturgiaRepository: LiturgiaRepository,
    private val streaksRepository: StreaksPreferencesRepository,
    private val dateProvider: DateProvider = DefaultDateProvider()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiturgiaUiState())
    val uiState: StateFlow<LiturgiaUiState> = _uiState.asStateFlow()

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
