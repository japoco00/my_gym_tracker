package com.jacopo.mygymtracker.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.jacopo.mygymtracker.domain.model.ThemeMode
import com.jacopo.mygymtracker.domain.model.UserSettings
import com.jacopo.mygymtracker.domain.model.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Persists [UserSettings] in Preferences DataStore. */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    /** Emits the current settings; unreadable storage falls back to defaults. */
    fun observeSettings(): Flow<UserSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences ->
            UserSettings(
                weightUnit = preferences[WEIGHT_UNIT]
                    ?.let { name -> WeightUnit.entries.find { it.name == name } }
                    ?: WeightUnit.KG,
                restSeconds = preferences[REST_SECONDS] ?: UserSettings.DEFAULT_REST_SECONDS,
                plates = WeightUnit.entries.associateWith { unit ->
                    preferences[platesKey(unit)]
                        ?.mapNotNull { it.toDoubleOrNull() }
                        ?.sortedDescending()
                        ?: unit.defaultPlates
                },
                themeMode = preferences[THEME_MODE]
                    ?.let { name -> ThemeMode.entries.find { it.name == name } }
                    ?: ThemeMode.SYSTEM,
                dynamicColor = preferences[DYNAMIC_COLOR] ?: true,
                autoStartRest = preferences[AUTO_START_REST] ?: true,
                keepScreenOn = preferences[KEEP_SCREEN_ON] ?: true,
                name = preferences[NAME] ?: "",
            )
        }

    suspend fun setWeightUnit(unit: WeightUnit) {
        dataStore.edit { it[WEIGHT_UNIT] = unit.name }
    }

    /** Stores the default rest duration, never shorter than one [UserSettings.REST_STEP_SECONDS] step. */
    suspend fun setRestSeconds(seconds: Int) {
        dataStore.edit { it[REST_SECONDS] = seconds.coerceAtLeast(UserSettings.REST_STEP_SECONDS) }
    }

    /** Adds [plate] to the plates owned in [unit], or removes it if already there. */
    suspend fun togglePlate(unit: WeightUnit, plate: Double) {
        dataStore.edit { preferences ->
            val key = platesKey(unit)
            val owned = preferences[key] ?: unit.defaultPlates.map { it.toString() }.toSet()
            val value = plate.toString()
            preferences[key] = if (value in owned) owned - value else owned + value
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setAutoStartRest(enabled: Boolean) {
        dataStore.edit { it[AUTO_START_REST] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[KEEP_SCREEN_ON] = enabled }
    }

    /** Stores [name] without surrounding blanks. */
    suspend fun setName(name: String) {
        dataStore.edit { it[NAME] = name.trim() }
    }

    /**
     * Replaces every setting at once, as when restoring a backup.
     * Plates outside a unit's [WeightUnit.plateOptions] are dropped; a unit without plates gets the defaults.
     */
    suspend fun replaceAll(settings: UserSettings) {
        dataStore.edit { preferences ->
            preferences[WEIGHT_UNIT] = settings.weightUnit.name
            preferences[REST_SECONDS] = settings.restSeconds.coerceAtLeast(UserSettings.REST_STEP_SECONDS)
            WeightUnit.entries.forEach { unit ->
                preferences[platesKey(unit)] = (settings.plates[unit] ?: unit.defaultPlates)
                    .filter { it in unit.plateOptions }
                    .map { it.toString() }
                    .toSet()
            }
            preferences[THEME_MODE] = settings.themeMode.name
            preferences[DYNAMIC_COLOR] = settings.dynamicColor
            preferences[AUTO_START_REST] = settings.autoStartRest
            preferences[KEEP_SCREEN_ON] = settings.keepScreenOn
            preferences[NAME] = settings.name.trim()
        }
    }

    private companion object {
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val REST_SECONDS = intPreferencesKey("rest_seconds")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AUTO_START_REST = booleanPreferencesKey("auto_start_rest")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val NAME = stringPreferencesKey("name")

        /** Plates as `Double.toString()` texts; a missing key means the [WeightUnit.defaultPlates]. */
        fun platesKey(unit: WeightUnit) = stringSetPreferencesKey("plates_${unit.name.lowercase()}")
    }
}