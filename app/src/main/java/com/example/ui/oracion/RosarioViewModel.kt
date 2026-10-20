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
    val misterioManual: MisterioRosario = MisterioRosario.AUTOMATICO,
    val oraciones: OracionesRosario = OracionesRosario(),
    val autoavance: Autoavance = Autoavance.APAGADO
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
            // preferencia guardada se usa la regla litúrgica (Automático). Las oraciones
            // opcionales vienen activas por defecto, como en la secuencia de la RN.
            val manual = MisterioRosario.desdeValor(
                userPreferencesRepository.rosarioMisterioManual.first()
            )
            val oraciones = OracionesRosario(
                fatima = userPreferencesRepository.rosarioFatima.first(),
                salve = userPreferencesRepository.rosarioSalve.first(),
                oracionFinal = userPreferencesRepository.rosarioOracionFinal.first()
            )
            val autoavance = Autoavance.desdeValor(
                userPreferencesRepository.rosarioAutoavance.first()
            )
            _uiState.update {
                it.copy(
                    pasos = generarPasosRosario(fechaSesion, manual, oraciones),
                    misterioManual = manual,
                    oraciones = oraciones,
                    autoavance = autoavance
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
                pasos = generarPasosRosario(hoy, it.misterioManual, it.oraciones),
                pasoIndex = 0,
                completado = false,
                isLoading = false
            )
        }
    }

    /**
     * Cambia los misterios del rezo (o vuelve a la regla litúrgica). Los pasos se regeneran
     * y el rezo vuelve al inicio: el índice guardado del día corresponde a otra lista. La
     * preferencia se persiste para las próximas sesiones y el progreso a medias se descarta.
     */
    fun setMisterioManual(misterio: MisterioRosario) {
        if (misterio == _uiState.value.misterioManual) return

        _uiState.update {
            it.copy(
                misterioManual = misterio,
                pasos = generarPasosRosario(fechaSesion, misterio, it.oraciones),
                pasoIndex = 0
            )
        }
        viewModelScope.launch {
            // El borrado va primero: quien espere la preferencia nueva (tests incluidos)
            // tiene la garantía de que el progreso del día ya quedó descartado.
            streaksRepository.limpiarRosarioProgreso()
            userPreferencesRepository.setRosarioMisterioManual(misterio.valor)
        }
    }

    /**
     * Incluye o quita una oración opcional: la Fátima tras cada decena, la Salve o la oración
     * final en el cierre. La lista de pasos cambia de longitud, así que se regenera y el rezo
     * vuelve al inicio: el índice guardado del día ya no corresponde. La preferencia se
     * persiste para las próximas sesiones y el progreso a medias se descarta sin tocar la
     * racha ni el total.
     */
    fun setOracionOpcional(opcion: OracionOpcional, incluida: Boolean) {
        val actual = _uiState.value.oraciones
        if (opcion.incluidaEn(actual) == incluida) return

        val nuevas = opcion.con(actual, incluida)
        _uiState.update {
            it.copy(
                oraciones = nuevas,
                pasos = generarPasosRosario(fechaSesion, it.misterioManual, nuevas),
                pasoIndex = 0
            )
        }
        viewModelScope.launch {
            // Mismo orden que con el misterio: primero el borrado y después la preferencia.
            streaksRepository.limpiarRosarioProgreso()
            when (opcion) {
                OracionOpcional.FATIMA -> userPreferencesRepository.setRosarioFatima(incluida)
                OracionOpcional.SALVE -> userPreferencesRepository.setRosarioSalve(incluida)
                OracionOpcional.ORACION_FINAL ->
                    userPreferencesRepository.setRosarioOracionFinal(incluida)
            }
        }
    }

    fun siguiente() = moverA(_uiState.value.pasoIndex + 1)

    fun anterior() = moverA(_uiState.value.pasoIndex - 1)

    /**
     * Cambia la velocidad del avance automático (o lo apaga). No toca los pasos ni el progreso:
     * solo define cuánto se espera antes de pasar solo al siguiente, y se recuerda entre
     * sesiones.
     */
    fun setAutoavance(velocidad: Autoavance) {
        if (velocidad == _uiState.value.autoavance) return

        _uiState.update { it.copy(autoavance = velocidad) }
        viewModelScope.launch {
            userPreferencesRepository.setRosarioAutoavance(velocidad.valor)
        }
    }

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
