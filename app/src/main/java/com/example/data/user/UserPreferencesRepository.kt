package com.example.data.user

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Handles user preferences such as font size multiplier (0.8 to 1.5, default 1.0, step 0.1).
 */
class UserPreferencesRepository(private val context: Context) {

    companion object {
        val FONT_SIZE_MULTIPLIER_KEY = floatPreferencesKey("font_size_multiplier")
        const val DEFAULT_FONT_SCALE = 1.0f
        const val MIN_FONT_SCALE = 0.8f
        const val MAX_FONT_SCALE = 1.5f
        const val FONT_STEP = 0.1f

        /**
         * Clamps and rounds a float value to 1 decimal place within [MIN_FONT_SCALE, MAX_FONT_SCALE].
         */
        fun sanitizeMultiplier(value: Float): Float {
            val clamped = value.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
            return (clamped * 10f).roundToInt() / 10f
        }

        fun increment(current: Float): Float {
            return sanitizeMultiplier(current + FONT_STEP)
        }

        fun decrement(current: Float): Float {
            return sanitizeMultiplier(current - FONT_STEP)
        }
    }

    val fontSizeMultiplier: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[FONT_SIZE_MULTIPLIER_KEY] ?: DEFAULT_FONT_SCALE
    }

    suspend fun setFontSizeMultiplier(multiplier: Float) {
        val sanitized = sanitizeMultiplier(multiplier)
        context.dataStore.edit { preferences ->
            preferences[FONT_SIZE_MULTIPLIER_KEY] = sanitized
        }
    }

    suspend fun increaseFontSize(current: Float) {
        setFontSizeMultiplier(increment(current))
    }

    suspend fun decreaseFontSize(current: Float) {
        setFontSizeMultiplier(decrement(current))
    }

    suspend fun resetFontSize() {
        setFontSizeMultiplier(DEFAULT_FONT_SCALE)
    }
}
