package com.jacopo.mygymtracker.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
            )
        }

    suspend fun setWeightUnit(unit: WeightUnit) {
        dataStore.edit { it[WEIGHT_UNIT] = unit.name }
    }

    /** Stores the default rest duration, never shorter than one [UserSettings.REST_STEP_SECONDS] step. */
    suspend fun setRestSeconds(seconds: Int) {
        dataStore.edit { it[REST_SECONDS] = seconds.coerceAtLeast(UserSettings.REST_STEP_SECONDS) }
    }

    private companion object {
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val REST_SECONDS = intPreferencesKey("rest_seconds")
    }
}