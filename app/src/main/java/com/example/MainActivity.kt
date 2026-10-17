package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.example.ui.common.ErrorState
import com.example.ui.common.LoadingState
import com.example.ui.navigation.MainScreen
import com.example.ui.theme.IglesiaDigitalTheme
import com.example.ui.theme.NavyBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Modo inmersivo (paridad con la RN): se ocultan la status bar y la
        // navigation bar; al deslizar desde el borde aparecen transitoriamente.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val appContainer = (application as IglesiaApplication).container

        setContent {
            IglesiaDigitalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NavyBackground
                ) {
                    AppInitializer(appContainer = appContainer)
                }
            }
        }
    }
}

@Composable
fun AppInitializer(appContainer: AppContainer) {
    var isInitialized by remember { mutableStateOf(false) }
    var initError by remember { mutableStateOf<String?>(null) }

    // Clave de reintento: al incrementarla, el LaunchedEffect vuelve a ejecutarse.
    // Sin ella, "Reintentar" limpiaba el error pero la inicialización no se relanzaba.
    var retryKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryKey) {
        initError = null
        isInitialized = false
        try {
            appContainer.iglesiaDatabaseHelper.initializeDatabase()
            isInitialized = true
        } catch (e: Exception) {
            initError = "Error al inicializar la base de datos: ${e.localizedMessage}"
        }
    }

    when {
        initError != null -> {
            ErrorState(
                message = initError!!,
                onRetry = { retryKey++ }
            )
        }
        !isInitialized -> {
            LoadingState(message = "Preparando las Sagradas Escrituras y el buscador...")
        }
        else -> {
            val navController = rememberNavController()
            MainScreen(
                appContainer = appContainer,
                navController = navController
            )
        }
    }
}
