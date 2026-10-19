package com.example.data.user

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.util.DateProvider
import com.example.util.DefaultDateProvider
import com.example.util.StreakStats
import com.example.util.calcularNuevaRacha
import com.example.util.calcularRacha
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.streaksDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_streaks")

/**
 * Progreso de un rezo interactivo a medias: la fecha en que se guardó y el paso alcanzado.
 * Se descarta al completar el rezo (y al día siguiente, porque la fecha no coincide).
 */
data class ProgresoRezo(
    val fechaIso: String?,
    val paso: Int?
)

class StreaksPreferencesRepository(
    private val context: Context,
    private val dateProvider: DateProvider = DefaultDateProvider()
) {
    companion object {
        val KEY_RACHA_ROSARIO_ULTIMA = stringPreferencesKey("racha_rosario_ultima")
        val KEY_RACHA_ROSARIO_COUNT = stringPreferencesKey("racha_rosario_count")
        val KEY_STATS_ROSARIO_TOTAL = stringPreferencesKey("stats_rosario_total")

        val KEY_RACHA_CORONILLA_ULTIMA = stringPreferencesKey("racha_coronilla_ultima")
        val KEY_RACHA_CORONILLA_COUNT = stringPreferencesKey("racha_coronilla_count")
        val KEY_STATS_CORONILLA_TOTAL = stringPreferencesKey("stats_coronilla_total")

        // Mismas claves que usa el `PrayerRunner` de la RN para reanudar el rezo del día.
        val KEY_ROSARIO_PROGRESO_FECHA = stringPreferencesKey("racha_rosario_ultima_saved_date")
        val KEY_ROSARIO_PROGRESO_PASO = stringPreferencesKey("racha_rosario_ultima_saved_step")
    }

    val streakStatsFlow: Flow<StreakStats> = context.streaksDataStore.data.map { prefs ->
        val today = dateProvider.today()

        val rU = prefs[KEY_RACHA_ROSARIO_ULTIMA]
        val rC = prefs[KEY_RACHA_ROSARIO_COUNT]
        val rT = prefs[KEY_STATS_ROSARIO_TOTAL]?.toIntOrNull() ?: 0
        val rachaR = calcularRacha(rU, rC, today)

        val cU = prefs[KEY_RACHA_CORONILLA_ULTIMA]
        val cC = prefs[KEY_RACHA_CORONILLA_COUNT]
        val cT = prefs[KEY_STATS_CORONILLA_TOTAL]?.toIntOrNull() ?: 0
        val rachaC = calcularRacha(cU, cC, today)

        StreakStats(
            rosarioTotal = rT,
            coronillaTotal = cT,
            rachaRosarioDias = rachaR,
            rachaCoronillaDias = rachaC,
            rosarioUltima = rU,
            coronillaUltima = cU
        )
    }

    suspend fun setRosarioStats(ultimaIso: String?, count: String?, total: String?) {
        context.streaksDataStore.edit { prefs ->
            if (ultimaIso != null) prefs[KEY_RACHA_ROSARIO_ULTIMA] = ultimaIso else prefs.remove(KEY_RACHA_ROSARIO_ULTIMA)
            if (count != null) prefs[KEY_RACHA_ROSARIO_COUNT] = count else prefs.remove(KEY_RACHA_ROSARIO_COUNT)
            if (total != null) prefs[KEY_STATS_ROSARIO_TOTAL] = total else prefs.remove(KEY_STATS_ROSARIO_TOTAL)
        }
    }

    suspend fun setCoronillaStats(ultimaIso: String?, count: String?, total: String?) {
        context.streaksDataStore.edit { prefs ->
            if (ultimaIso != null) prefs[KEY_RACHA_CORONILLA_ULTIMA] = ultimaIso else prefs.remove(KEY_RACHA_CORONILLA_ULTIMA)
            if (count != null) prefs[KEY_RACHA_CORONILLA_COUNT] = count else prefs.remove(KEY_RACHA_CORONILLA_COUNT)
            if (total != null) prefs[KEY_STATS_CORONILLA_TOTAL] = total else prefs.remove(KEY_STATS_CORONILLA_TOTAL)
        }
    }

    /** Progreso guardado del rosario (fecha + paso), para reanudar un rezo a medias del día. */
    suspend fun getRosarioProgreso(): ProgresoRezo {
        val prefs = context.streaksDataStore.data.first()
        return ProgresoRezo(
            fechaIso = prefs[KEY_ROSARIO_PROGRESO_FECHA],
            paso = prefs[KEY_ROSARIO_PROGRESO_PASO]?.toIntOrNull()
        )
    }

    suspend fun guardarRosarioProgreso(fechaIso: String, paso: Int) {
        context.streaksDataStore.edit { prefs ->
            prefs[KEY_ROSARIO_PROGRESO_FECHA] = fechaIso
            prefs[KEY_ROSARIO_PROGRESO_PASO] = paso.toString()
        }
    }

    /**
     * Descarta el progreso del día sin tocar la racha ni el total. Se usa al cambiar la
     * selección de misterios: los 90 pasos cambian y el índice guardado deja de ser válido.
     */
    suspend fun limpiarRosarioProgreso() {
        context.streaksDataStore.edit { prefs ->
            prefs.remove(KEY_ROSARIO_PROGRESO_FECHA)
            prefs.remove(KEY_ROSARIO_PROGRESO_PASO)
        }
    }

    /**
     * Registra el rosario completado en [hoy]: actualiza la racha con la misma regla que la
     * RN ([calcularNuevaRacha]), suma el total histórico y descarta el progreso a medias.
     * Todo en una sola transacción para no dejar estados intermedios.
     */
    suspend fun registrarRosarioCompletado(hoy: LocalDate) {
        context.streaksDataStore.edit { prefs ->
            val racha = calcularNuevaRacha(
                prefs[KEY_RACHA_ROSARIO_ULTIMA],
                prefs[KEY_RACHA_ROSARIO_COUNT],
                hoy
            )
            val total = (prefs[KEY_STATS_ROSARIO_TOTAL]?.toIntOrNull() ?: 0) + 1

            prefs[KEY_RACHA_ROSARIO_ULTIMA] = hoy.toString()
            prefs[KEY_RACHA_ROSARIO_COUNT] = racha.toString()
            prefs[KEY_STATS_ROSARIO_TOTAL] = total.toString()
            prefs.remove(KEY_ROSARIO_PROGRESO_FECHA)
            prefs.remove(KEY_ROSARIO_PROGRESO_PASO)
        }
    }
}
