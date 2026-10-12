package com.example.ui.biblia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.LibroBiblia
import com.example.data.db.VersiculoBiblia
import com.example.data.repository.BibliaRepository
import com.example.data.user.Favorito
import com.example.data.user.UserPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FiltroTestamento {
    TODOS,
    ANTIGUO,
    NUEVO
}

sealed interface LibrosUiState {
    data object Loading : LibrosUiState
    data class Success(val libros: List<LibroBiblia>) : LibrosUiState
    data class Error(val message: String) : LibrosUiState
}

sealed interface CapitulosUiState {
    data object Loading : CapitulosUiState
    data class Success(val capitulos: List<Int>) : CapitulosUiState
    data class Error(val message: String) : CapitulosUiState
}

sealed interface LecturaUiState {
    data object Loading : LecturaUiState
    data class Success(
        val versiculos: List<VersiculoBiblia>,
        val favoritosMap: Map<String, Favorito>
    ) : LecturaUiState
    data class Error(val message: String) : LecturaUiState
}

sealed interface BusquedaUiState {
    data object Idle : BusquedaUiState
    data object Searching : BusquedaUiState
    data class Success(val resultados: List<VersiculoBiblia>) : BusquedaUiState
    data class Error(val message: String) : BusquedaUiState
}

class BibliaViewModel(
    private val repository: BibliaRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // --- State: Libros & Filters ---
    private val _librosState = MutableStateFlow<LibrosUiState>(LibrosUiState.Loading)
    val librosState: StateFlow<LibrosUiState> = _librosState.asStateFlow()

    private val _filtroTestamento = MutableStateFlow(FiltroTestamento.TODOS)
    val filtroTestamento: StateFlow<FiltroTestamento> = _filtroTestamento.asStateFlow()

    // --- State: Search ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _busquedaState = MutableStateFlow<BusquedaUiState>(BusquedaUiState.Idle)
    val busquedaState: StateFlow<BusquedaUiState> = _busquedaState.asStateFlow()

    private var searchJob: Job? = null

    // --- State: Capítulos ---
    private val _capitulosState = MutableStateFlow<CapitulosUiState>(CapitulosUiState.Loading)
    val capitulosState: StateFlow<CapitulosUiState> = _capitulosState.asStateFlow()

    // --- State: Lectura ---
    private val _lecturaState = MutableStateFlow<LecturaUiState>(LecturaUiState.Loading)
    val lecturaState: StateFlow<LecturaUiState> = _lecturaState.asStateFlow()

    // Current open reading parameters
    private var currentLibroLectura: String? = null
    private var currentCapituloLectura: Int? = null

    // Font size multiplier
    val fontSizeMultiplier: StateFlow<Float> = preferencesRepository.fontSizeMultiplier
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferencesRepository.DEFAULT_FONT_SCALE
        )

    init {
        loadLibros()
        observeFavoritos()
    }

    fun loadLibros() {
        viewModelScope.launch {
            _librosState.value = LibrosUiState.Loading
            try {
                val libros = repository.getLibros()
                if (libros.isEmpty()) {
                    _librosState.value = LibrosUiState.Error("No se pudieron cargar los libros.")
                } else {
                    _librosState.value = LibrosUiState.Success(libros)
                }
            } catch (e: Exception) {
                _librosState.value = LibrosUiState.Error("Error al cargar libros: ${e.localizedMessage}")
            }
        }
    }

    fun setFiltroTestamento(filtro: FiltroTestamento) {
        _filtroTestamento.value = filtro
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.isBlank()) {
            _busquedaState.value = BusquedaUiState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            _busquedaState.value = BusquedaUiState.Searching
            try {
                val resultados = repository.searchVersiculos(query)
                _busquedaState.value = BusquedaUiState.Success(resultados)
            } catch (e: Exception) {
                _busquedaState.value = BusquedaUiState.Error("Error en la búsqueda: ${e.localizedMessage}")
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _busquedaState.value = BusquedaUiState.Idle
        searchJob?.cancel()
    }

    fun loadCapitulos(libro: String) {
        viewModelScope.launch {
            _capitulosState.value = CapitulosUiState.Loading
            try {
                val caps = repository.getCapitulos(libro)
                if (caps.isEmpty()) {
                    _capitulosState.value = CapitulosUiState.Error("No se encontraron capítulos para $libro.")
                } else {
                    _capitulosState.value = CapitulosUiState.Success(caps)
                }
            } catch (e: Exception) {
                _capitulosState.value = CapitulosUiState.Error("Error al cargar capítulos: ${e.localizedMessage}")
            }
        }
    }

    fun loadLectura(libro: String, capitulo: Int) {
        currentLibroLectura = libro
        currentCapituloLectura = capitulo
        viewModelScope.launch {
            _lecturaState.value = LecturaUiState.Loading
            try {
                val versiculos = repository.getVersiculos(libro, capitulo)
                if (versiculos.isEmpty()) {
                    _lecturaState.value = LecturaUiState.Error("No se encontraron versículos en $libro $capitulo.")
                } else {
                    // Fetch existing favorites
                    _lecturaState.value = LecturaUiState.Success(
                        versiculos = versiculos,
                        favoritosMap = emptyMap()
                    )
                }
            } catch (e: Exception) {
                _lecturaState.value = LecturaUiState.Error("Error al cargar la lectura: ${e.localizedMessage}")
            }
        }
    }

    private fun observeFavoritos() {
        viewModelScope.launch {
            repository.getFavoritosFlow().collect { favoritosList ->
                val favMap = favoritosList.associateBy { it.id }
                val currentState = _lecturaState.value
                if (currentState is LecturaUiState.Success) {
                    _lecturaState.value = currentState.copy(favoritosMap = favMap)
                }
            }
        }
    }

    fun toggleFavorito(versiculo: VersiculoBiblia, colorHex: String) {
        viewModelScope.launch {
            repository.toggleFavorito(versiculo, colorHex)
        }
    }

    fun increaseFontSize() {
        viewModelScope.launch {
            preferencesRepository.increaseFontSize(fontSizeMultiplier.value)
        }
    }

    fun decreaseFontSize() {
        viewModelScope.launch {
            preferencesRepository.decreaseFontSize(fontSizeMultiplier.value)
        }
    }

    fun resetFontSize() {
        viewModelScope.launch {
            preferencesRepository.resetFontSize()
        }
    }
}

class BibliaViewModelFactory(
    private val repository: BibliaRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BibliaViewModel::class.java)) {
            return BibliaViewModel(repository, preferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
