package com.jacopo.mygymtracker.ui.bodyweight

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jacopo.mygymtracker.data.InMemoryPreferencesDataStore
import com.jacopo.mygymtracker.data.createTestDatabase
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.repository.BodyWeightRepository
import com.jacopo.mygymtracker.data.repository.SettingsRepository
import com.jacopo.mygymtracker.domain.model.BodyWeight
import com.jacopo.mygymtracker.ui.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class BodyWeightViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: AppDatabase
    private lateinit var bodyWeights: BodyWeightRepository
    private val viewModels = ViewModelStore()

    private val monday = LocalDate.of(2025, 7, 14)
    private val tuesday = monday.plusDays(1)

    @Before
    fun setUp() {
        database = createTestDatabase()
        bodyWeights = BodyWeightRepository(database, database.bodyWeightDao())
    }

    @After
    fun tearDown() {
        viewModels.clear()
        database.close()
    }

    @Test
    fun save_movesTheEditedEntryAndClosesTheEditor() = runTest {
        bodyWeights.save(BodyWeight(monday, 80.0))
        val viewModel = loadedViewModel()
        viewModel.openEditor(BodyWeight(monday, 80.0))

        viewModel.save(tuesday, 79.5)

        val state = viewModel.uiState.first { it.entries.firstOrNull()?.date == tuesday }
        assertEquals(listOf(BodyWeight(tuesday, 79.5)), state.entries)
        assertNull(state.editor)
    }

    private suspend fun loadedViewModel(): BodyWeightViewModel {
        val factory = viewModelFactory {
            initializer { BodyWeightViewModel(bodyWeights, SettingsRepository(InMemoryPreferencesDataStore())) }
        }
        val viewModel = ViewModelProvider.create(viewModels, factory)[BodyWeightViewModel::class]
        viewModel.uiState.first { !it.isLoading }
        return viewModel
    }
}
