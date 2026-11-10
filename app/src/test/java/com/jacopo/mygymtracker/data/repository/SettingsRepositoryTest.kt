package com.jacopo.mygymtracker.data.repository

import com.jacopo.mygymtracker.data.InMemoryPreferencesDataStore
import com.jacopo.mygymtracker.domain.model.ThemeMode
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
        repository.setThemeMode(ThemeMode.DARK)
        repository.setDynamicColor(false)
        repository.setAutoStartRest(false)
        repository.setKeepScreenOn(false)
        repository.setName("  Jacopo ")

        val expected = UserSettings(
            weightUnit = WeightUnit.LB,
            restSeconds = 120,
            themeMode = ThemeMode.DARK,
            dynamicColor = false,
            autoStartRest = false,
            keepScreenOn = false,
            name = "Jacopo",
        )
        assertEquals(expected, repository.observeSettings().first())
    }

    @Test
    fun restSeconds_neverBelowOneStep() = runTest {
        repository.setRestSeconds(0)

        assertEquals(UserSettings.REST_STEP_SECONDS, repository.observeSettings().first().restSeconds)
    }

    @Test
    fun togglePlate_changesOnlyThatUnitAndKeepsHeaviestFirst() = runTest {
        repository.togglePlate(WeightUnit.KG, 25.0)
        repository.togglePlate(WeightUnit.KG, 0.5)

        val plates = repository.observeSettings().first().plates
        assertEquals(listOf(20.0, 15.0, 10.0, 5.0, 2.5, 1.25, 0.5), plates[WeightUnit.KG])
        assertEquals(WeightUnit.LB.defaultPlates, plates[WeightUnit.LB])
    }

    @Test
    fun togglePlate_canRemoveEveryPlate() = runTest {
        WeightUnit.LB.defaultPlates.forEach { repository.togglePlate(WeightUnit.LB, it) }

        val settings = repository.observeSettings().first().copy(weightUnit = WeightUnit.LB)
        assertEquals(emptyList<Double>(), settings.currentPlates)
    }
}
