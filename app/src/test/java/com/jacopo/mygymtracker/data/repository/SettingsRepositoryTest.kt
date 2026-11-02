package com.jacopo.mygymtracker.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.jacopo.mygymtracker.domain.model.UserSettings
import com.jacopo.mygymtracker.domain.model.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryTest {

    private val repository = SettingsRepository(InMemoryPreferencesDataStore())

    @Test
    fun emitsDefaults_whenNothingStored() = runTest {
        assertEquals(UserSettings(), repository.observeSettings().first())
    }

    @Test
    fun persistsUpdates() = runTest {
        repository.setWeightUnit(WeightUnit.LB)
        repository.setRestSeconds(120)

        assertEquals(UserSettings(WeightUnit.LB, 120), repository.observeSettings().first())
    }

    @Test
    fun restSeconds_neverBelowOneStep() = runTest {
        repository.setRestSeconds(0)

        assertEquals(UserSettings.REST_STEP_SECONDS, repository.observeSettings().first().restSeconds)
    }
}

/** File-free DataStore: the real one cannot overwrite its file on a Windows JVM. */
private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        transform(state.value).also { state.value = it }
}