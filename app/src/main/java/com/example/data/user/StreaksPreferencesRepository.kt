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
import com.example.util.calcularRacha
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.streaksDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_streaks")

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
}
