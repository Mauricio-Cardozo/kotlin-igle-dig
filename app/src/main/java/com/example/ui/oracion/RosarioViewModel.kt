package com.example.ui.oracion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.user.StreaksPreferencesRepository
import com.example.data.user.UserPreferencesRepository
import com.example.util.DateProvider
import com.example.util.DefaultDateProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class RosarioUiState(
    val isLoading: Boolean = true,
    val pasos: List<RosarioStep> = emptyList(),
    val pasoIndex: Int = 0,
    val completado: Boolean = false,
    val misterioManual: MisterioRosario = MisterioRosario.AUTOMATICO
) {
    val paso: RosarioStep? get() = pasos.getOrNull(pasoIndex)
    val total: Int get() = pasos.size
}

/**
 * Estado del rezo interactivo del rosario. Replica al `PrayerRunner` de la RN: restaura el
 * paso guardado del día, persiste cada avance y registra racha + total al completar (una
 * sola vez por corrida).
 */
class RosarioViewModel(
    private val streaksRepository: StreaksPreferencesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dateProvider: DateProvider = DefaultDateProvider()
) : ViewModel() {

    companion object {
        /** Id del último paso de [generarPasosRosario]; dispara el registro de la racha. */
        const val PASO_COMPLETADO_ID = "completado"
    }

    private val _uiState = MutableStateFlow(RosarioUiState())
    val uiState: StateFlow<RosarioUiState> = _uiState.asStateFlow()

    val fontSizeMultiplier: StateFlow<Float> = userPreferencesRepository.fontSizeMultiplier
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferencesRepository.DEFAULT_FONT_SCALE)

    private var fechaSesion: LocalDate = dateProvider.today()

    init {
        viewModelScope.launch {
            // La selección manual define los misterios antes de generar los pasos; sin
            // preferencia guardada se usa la regla litúrgica (Automático).
            val manual = MisterioRosario.desdeValor(
                userPreferencesRepository.rosarioMisterioManual.first()
            )
            _uiState.update {
                it.copy(
                    pasos = generarPasosRosario(fechaSesion, manual),
                    misterioManual = manual
                )
            }

            // Reanuda el rezo a medias si el paso guardado es de hoy. El progreso se borra al
            // completar, así que solo aparece cuando quedó un rezo sin terminar.
            val progreso = streaksRepository.getRosarioProgreso()
            val total = _uiState.value.total
            val paso = progreso.paso
            if (progreso.fechaIso == fechaSesion.toString() && paso != null && paso > 0 && paso < total) {
                _uiState.update { it.copy(pasoIndex = paso) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * La pantalla RN se vuelve a montar cada vez que se entra: regenera los pasos del día y
     * arranca de cero si el rezo anterior ya se había completado. Se replica ese comportamiento
     * cuando cambió la fecha de sesión o el rezo ya terminó; si no, se conserva el paso actual
     * (que ya está en memoria).
     */
    fun onScreenEntered() {
        val hoy = dateProvider.today()
        if (hoy == fechaSesion && !_uiState.value.completado) return

        fechaSesion = hoy
        _uiState.update {
            it.copy(
                pasos = generarPasosRosario(hoy, it.misterioManual),
                pasoIndex = 0,
                completado = false,
                isLoading = false
            )
        }
    }

    /**
     * Cambia los misterios del rezo (o vuelve a la regla litúrgica). Los 90 pasos se regeneran
     * y el rezo vuelve al inicio: el índice guardado del día corresponde a otra lista. La
     * preferencia se persiste para las próximas sesiones y el progreso a medias se descarta.
     */
    fun setMisterioManual(misterio: MisterioRosario) {
        if (misterio == _uiState.value.misterioManual) return

        _uiState.update {
            it.copy(
                misterioManual = misterio,
                pasos = generarPasosRosario(fechaSesion, misterio),
                pasoIndex = 0
            )
        }
        viewModelScope.launch {
            userPreferencesRepository.setRosarioMisterioManual(misterio.valor)
            streaksRepository.limpiarRosarioProgreso()
        }
    }

    fun siguiente() = moverA(_uiState.value.pasoIndex + 1)

    fun anterior() = moverA(_uiState.value.pasoIndex - 1)

    private fun moverA(index: Int) {
        val estado = _uiState.value
        val destino = index.coerceIn(0, estado.total - 1)
        if (destino == estado.pasoIndex) return

        _uiState.update { it.copy(pasoIndex = destino) }
        guardarProgreso(destino)
    }

    private fun guardarProgreso(paso: Int) {
        // Después de completar la RN deja de guardar: evita reanudar un rezo ya terminado.
        if (_uiState.value.completado) return
        val fecha = fechaSesion.toString()
        viewModelScope.launch { streaksRepository.guardarRosarioProgreso(fecha, paso) }
    }

    /**
     * Marca el rezo como completado y actualiza racha/estadísticas. Es idempotente dentro de
     * la misma corrida: volver a rezar con "Rezar de nuevo" no cuenta dos veces.
     */
    fun marcarCompletado() {
        val estado = _uiState.value
        if (estado.completado || estado.paso?.id != PASO_COMPLETADO_ID) return

        _uiState.update { it.copy(completado = true) }
        viewModelScope.launch { streaksRepository.registrarRosarioCompletado(dateProvider.today()) }
    }

    /** "Rezar de nuevo" del footer final: vuelve al inicio sin reiniciar la racha. */
    fun rezarDeNuevo() {
        _uiState.update { it.copy(pasoIndex = 0) }
    }

    fun decreaseFontSize() {
        viewModelScope.launch {
            userPreferencesRepository.decreaseFontSize(fontSizeMultiplier.value)
        }
    }

    fun increaseFontSize() {
        viewModelScope.launch {
            userPreferencesRepository.increaseFontSize(fontSizeMultiplier.value)
        }
    }

    fun resetFontSize() {
        viewModelScope.launch {
            userPreferencesRepository.resetFontSize()
        }
    }
}

class RosarioViewModelFactory(
    private val streaksRepository: StreaksPreferencesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dateProvider: DateProvider = DefaultDateProvider()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RosarioViewModel::class.java)) {
            return RosarioViewModel(streaksRepository, userPreferencesRepository, dateProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
    }
}
