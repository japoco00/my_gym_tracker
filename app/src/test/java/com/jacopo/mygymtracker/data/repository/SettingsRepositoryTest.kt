package com.jacopo.mygymtracker.data.repository

import com.jacopo.mygymtracker.data.InMemoryPreferencesDataStore
import com.jacopo.mygymtracker.domain.model.UserSettings
import com.jacopo.mygymtracker.domain.model.WeightUnit
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
