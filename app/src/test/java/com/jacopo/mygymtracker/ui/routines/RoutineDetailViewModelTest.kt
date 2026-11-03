package com.jacopo.mygymtracker.ui.routines

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jacopo.mygymtracker.data.InMemoryPreferencesDataStore
import com.jacopo.mygymtracker.data.createTestDatabase
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.repository.ExerciseRepository
import com.jacopo.mygymtracker.data.repository.RoutineRepository
import com.jacopo.mygymtracker.data.repository.SettingsRepository
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.ui.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutineDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: AppDatabase
    private lateinit var routines: RoutineRepository
    private lateinit var exercises: ExerciseRepository
    private val viewModels = ViewModelStore()

    @Before
    fun setUp() {
        database = createTestDatabase()
        routines = RoutineRepository(database, database.routineDao(), database.exerciseDao())
        exercises = ExerciseRepository(database, database.exerciseDao())
    }

    @After
    fun tearDown() {
        viewModels.clear()
        database.close()
    }

    @Test
    fun addNewExercise_isSavedWithTheRoutineAndJoinsTheCatalog() = runTest {
        val viewModel = loadedViewModel()
        viewModel.updateRoutineName("Legs")
        viewModel.openNewExerciseSheet()

        viewModel.addNewExercise(" Pistol Squat ", Category.LEGS, TrackingType.REPS)
        viewModel.saveRoutine(untitledName = "Untitled")
        viewModel.uiState.first { it.isClosed }

        assertFalse(viewModel.uiState.value.isNewExerciseSheetOpen)
        val saved = routines.observeRoutines().first().single()
        val exercise = saved.exercises.single()
        assertEquals("Legs", saved.name)
        assertEquals("Pistol Squat", exercise.exercise.name)
        assertEquals(TrackingType.REPS, exercise.exercise.trackingType)
        assertEquals(3, exercise.sets.size)
        assertEquals(listOf("Pistol Squat"), exercises.observeCatalog().first().map { it.name })
    }

    @Test
    fun isNameTaken_ignoresCaseAcrossTheCatalogAndNewExercises() {
        val state = RoutineDetailUiState(
            catalogExercises = listOf(Exercise(id = 1, name = "Bench Press", category = Category.CHEST)),
            exercises = listOf(RoutineExerciseUiState(name = "Pistol Squat")),
        )

        assertTrue(state.isNameTaken(" bench press"))
        assertTrue(state.isNameTaken("PISTOL SQUAT"))
        assertFalse(state.isNameTaken("Squat"))
    }

    private suspend fun loadedViewModel(): RoutineDetailViewModel {
        val factory = viewModelFactory {
            initializer {
                RoutineDetailViewModel(
                    routineId = null,
                    routineRepository = routines,
                    exerciseRepository = exercises,
                    settingsRepository = SettingsRepository(InMemoryPreferencesDataStore()),
                )
            }
        }
        val viewModel = ViewModelProvider.create(viewModels, factory)[RoutineDetailViewModel::class]
        viewModel.uiState.first { !it.isLoading }
        return viewModel
    }
}
