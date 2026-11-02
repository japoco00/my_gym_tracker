package com.jacopo.mygymtracker.ui.history

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jacopo.mygymtracker.data.InMemoryPreferencesDataStore
import com.jacopo.mygymtracker.data.createTestDatabase
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.newExercise
import com.jacopo.mygymtracker.data.repository.RoutineRepository
import com.jacopo.mygymtracker.data.repository.SettingsRepository
import com.jacopo.mygymtracker.data.repository.WorkoutRepository
import com.jacopo.mygymtracker.data.routineOf
import com.jacopo.mygymtracker.data.withAllSetsCompleted
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.PlannedSet
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
class WorkoutDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: AppDatabase
    private lateinit var workouts: WorkoutRepository
    private val viewModels = ViewModelStore()

    @Before
    fun setUp() {
        database = createTestDatabase()
        workouts = WorkoutRepository(database, database.workoutDao(), database.exerciseDao())
    }

    @After
    fun tearDown() {
        viewModels.clear()
        database.close()
    }

    @Test
    fun saveEdits_storesChangesAndLeavesEditMode() = runTest {
        val sessionId = finishedWorkout()
        val viewModel = editingViewModel(sessionId)
        val squat = viewModel.uiState.value.editedExercises!!.single()
        viewModel.updateSetReps(squat.key, squat.sets.first().key, 7)
        viewModel.removeSet(squat.key, squat.sets.last().key)

        viewModel.saveEdits()

        val state = viewModel.uiState.first { !it.isEditing }
        val expected = listOf(LoggedSet(7, 100.0, isCompleted = true))
        assertEquals(expected, state.workout!!.exercises.single().sets)
        assertEquals(expected, workouts.getWorkout(sessionId)!!.exercises.single().sets)
    }

    @Test
    fun cancelEditing_dropsChanges() = runTest {
        val sessionId = finishedWorkout()
        val viewModel = editingViewModel(sessionId)
        val before = viewModel.uiState.value.workout
        val squat = viewModel.uiState.value.editedExercises!!.single()
        viewModel.updateSetReps(squat.key, squat.sets.first().key, 7)

        viewModel.cancelEditing()

        assertFalse(viewModel.uiState.value.isEditing)
        assertEquals(before, viewModel.uiState.value.workout)
        assertEquals(before, workouts.getWorkout(sessionId))
    }

    @Test
    fun saveEdits_withoutSets_staysInEditMode() = runTest {
        val sessionId = finishedWorkout()
        val viewModel = editingViewModel(sessionId)
        val before = viewModel.uiState.value.workout
        val squat = viewModel.uiState.value.editedExercises!!.single()
        squat.sets.forEach { viewModel.removeSet(squat.key, it.key) }

        viewModel.saveEdits()

        assertFalse(viewModel.uiState.value.canSave)
        assertTrue(viewModel.uiState.value.isEditing)
        assertEquals(before, workouts.getWorkout(sessionId))
    }

    /** A finished workout with two completed squat sets of 5 x 100 kg. */
    private suspend fun finishedWorkout(): Int {
        val routines = RoutineRepository(database, database.routineDao(), database.exerciseDao())
        val routineId = routines.saveRoutine(
            routineOf("Push", newExercise("Squat") to listOf(PlannedSet(5, 100.0), PlannedSet(5, 100.0))),
        )
        val sessionId = workouts.startWorkout(routines.getRoutine(routineId)!!)!!
        workouts.finishWorkout(workouts.getWorkout(sessionId)!!.withAllSetsCompleted())
        return sessionId
    }

    /** A loaded detail screen, already in edit mode. */
    private suspend fun editingViewModel(sessionId: Int): WorkoutDetailViewModel {
        val factory = viewModelFactory {
            initializer {
                WorkoutDetailViewModel(
                    sessionId = sessionId,
                    workoutRepository = workouts,
                    settingsRepository = SettingsRepository(InMemoryPreferencesDataStore()),
                )
            }
        }
        val viewModel = ViewModelProvider.create(viewModels, factory)[WorkoutDetailViewModel::class]
        viewModel.uiState.first { it.workout != null }
        viewModel.startEditing()
        return viewModel
    }
}
