package com.example.ui.biblia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.BibliaVersion
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
    data class Success(
        val resultados: List<VersiculoBiblia>,
        /** true cuando puede haber más resultados además de los cargados. */
        val hayMas: Boolean = false,
        /** true mientras se carga la página siguiente. */
        val cargandoMas: Boolean = false
    ) : BusquedaUiState
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

    // --- State: Bible version (Pueblo de Dios / Navarra) ---
    private val _bibliaVersion = MutableStateFlow(BibliaVersion.DEFAULT)
    val bibliaVersion: StateFlow<BibliaVersion> = _bibliaVersion.asStateFlow()

    // --- State: Search ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _busquedaState = MutableStateFlow<BusquedaUiState>(BusquedaUiState.Idle)
    val busquedaState: StateFlow<BusquedaUiState> = _busquedaState.asStateFlow()

    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null

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
        observeVersion()
        loadLibros()
        observeFavoritos()
    }

    /**
     * Observa la versión bíblica persistida y recarga la lista de libros (y la búsqueda activa)
     * cuando cambia. La primera emisión con la versión por defecto no provoca recarga extra.
     */
    private fun observeVersion() {
        viewModelScope.launch {
            preferencesRepository.bibliaVersion.collect { key ->
                val version = BibliaVersion.fromKey(key)
                if (version != _bibliaVersion.value) {
                    _bibliaVersion.value = version
                    loadLibros()
                    val query = _searchQuery.value
                    if (query.isNotBlank()) {
                        onSearchQueryChanged(query)
                    }
                }
            }
        }
    }

    /**
     * Cambia la versión bíblica activa. Se persiste y [observeVersion] se encarga de recargar.
     */
    fun setBibliaVersion(version: BibliaVersion) {
        if (version == _bibliaVersion.value) return
        viewModelScope.launch {
            preferencesRepository.setBibliaVersion(version.key)
        }
    }

    fun loadLibros() {
        viewModelScope.launch {
            _librosState.value = LibrosUiState.Loading
            try {
                val libros = repository.getLibros(_bibliaVersion.value)
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
        loadMoreJob?.cancel()

        if (query.isBlank()) {
            _busquedaState.value = BusquedaUiState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            _busquedaState.value = BusquedaUiState.Searching
            try {
                val resultados = repository.searchVersiculos(query, _bibliaVersion.value)
                _busquedaState.value = BusquedaUiState.Success(
                    resultados = resultados,
                    hayMas = resultados.size == BibliaRepository.SEARCH_PAGE_SIZE
                )
            } catch (e: Exception) {
                _busquedaState.value = BusquedaUiState.Error("Error en la búsqueda: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Carga la página siguiente de resultados y la agrega debajo de lo ya mostrado.
     * Se ignora si no hay más páginas o si ya hay una carga en curso.
     */
    fun loadMoreResultados() {
        val actual = _busquedaState.value
        if (actual !is BusquedaUiState.Success || !actual.hayMas || actual.cargandoMas) return
        // Si hay un debounce o una búsqueda en curso, esperar: la página se pediría con el
        // offset de resultados que ya no corresponden a la consulta actual.
        if (searchJob?.isActive == true) return
        val query = _searchQuery.value
        if (query.isBlank()) return

        loadMoreJob = viewModelScope.launch {
            _busquedaState.value = actual.copy(cargandoMas = true)
            try {
                val nuevos = repository.searchVersiculos(
                    rawQuery = query,
                    version = _bibliaVersion.value,
                    offset = actual.resultados.size
                )
                _busquedaState.value = BusquedaUiState.Success(
                    resultados = actual.resultados + nuevos,
                    hayMas = nuevos.size == BibliaRepository.SEARCH_PAGE_SIZE
                )
            } catch (e: Exception) {
                // Conserva lo ya mostrado; solo deja de girar el indicador.
                _busquedaState.value = actual.copy(cargandoMas = false)
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _busquedaState.value = BusquedaUiState.Idle
        searchJob?.cancel()
        loadMoreJob?.cancel()
    }

    fun loadCapitulos(libro: String) {
        viewModelScope.launch {
            _capitulosState.value = CapitulosUiState.Loading
            try {
                val caps = repository.getCapitulos(libro, _bibliaVersion.value)
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

    // Cached map of all user favorites for immediate lookup
    private var cachedFavoritosMap: Map<String, Favorito> = emptyMap()

    fun loadLectura(libro: String, capitulo: Int) {
        currentLibroLectura = libro
        currentCapituloLectura = capitulo
        viewModelScope.launch {
            _lecturaState.value = LecturaUiState.Loading
            try {
                val versiculos = repository.getVersiculos(libro, capitulo, _bibliaVersion.value)
                if (versiculos.isEmpty()) {
                    _lecturaState.value = LecturaUiState.Error("No se encontraron versículos en $libro $capitulo.")
                } else {
                    _lecturaState.value = LecturaUiState.Success(
                        versiculos = versiculos,
                        favoritosMap = cachedFavoritosMap
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
                cachedFavoritosMap = favMap
                val currentState = _lecturaState.value
                if (currentState is LecturaUiState.Success) {
                    _lecturaState.value = currentState.copy(favoritosMap = favMap)
                }
            }
        }
    }

    fun toggleFavorito(versiculo: VersiculoBiblia, colorHex: String) {
        viewModelScope.launch {
            repository.toggleFavorito(versiculo, colorHex, _bibliaVersion.value)
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
