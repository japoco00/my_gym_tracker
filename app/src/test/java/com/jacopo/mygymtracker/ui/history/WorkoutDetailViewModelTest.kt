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
import com.jacopo.mygymtracker.data.repository.ExerciseRepository
import com.jacopo.mygymtracker.data.repository.RoutineRepository
import com.jacopo.mygymtracker.data.repository.SettingsRepository
import com.jacopo.mygymtracker.data.repository.WorkoutRepository
import com.jacopo.mygymtracker.data.routineOf
import com.jacopo.mygymtracker.data.withAllSetsCompleted
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.PlannedSet
import com.jacopo.mygymtracker.domain.model.SetType
import com.jacopo.mygymtracker.ui.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

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
        viewModel.updateSetType(squat.key, squat.sets.first().key, SetType.FAILURE)
        viewModel.updateSetRpe(squat.key, squat.sets.first().key, 9.5)
        viewModel.removeSet(squat.key, squat.sets.last().key)

        viewModel.saveEdits()

        val state = viewModel.uiState.first { !it.isEditing }
        val expected = listOf(LoggedSet(7, 100.0, isCompleted = true, type = SetType.FAILURE, rpe = 9.5))
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

    @Test
    fun prSetIndexes_markTheSetThatBeatsEarlierWorkouts() = runTest {
        finishedWorkout(startedAt = Instant.EPOCH)
        val sessionId = finishedWorkout(startedAt = Instant.EPOCH.plusSeconds(86_400))
        val viewModel = editingViewModel(sessionId)
        assertEquals(listOf(null), viewModel.uiState.value.prSetIndexes)
        val squat = viewModel.uiState.value.editedExercises!!.single()

        viewModel.updateSetWeight(squat.key, squat.sets.last().key, 105.0)
        viewModel.saveEdits()

        assertEquals(listOf(1), viewModel.uiState.first { !it.isEditing }.prSetIndexes)
    }

    @Test
    fun addExercises_appendsNewOnesAndSkipsDuplicates() = runTest {
        val sessionId = finishedWorkout()
        val viewModel = editingViewModel(sessionId)
        val squat = viewModel.uiState.value.editedExercises!!.single().exercise
        val bench = ExerciseRepository(database, database.exerciseDao()).let { repository ->
            repository.saveExercise(newExercise("Bench"))
            repository.observeCatalog().first().single { it.name == "Bench" }
        }

        viewModel.addExercises(listOf(squat, bench))

        val edited = viewModel.uiState.first { it.editedExercises!!.size == 2 }.editedExercises!!
        assertEquals(listOf("Squat", "Bench"), edited.map { it.exercise.name })
        assertEquals(1, edited.last().sets.size)
        assertNull(viewModel.uiState.value.editingSet)
    }

    /** A finished workout with two completed squat sets of 5 x 100 kg. */
    private suspend fun finishedWorkout(startedAt: Instant = Instant.now()): Int {
        val routines = RoutineRepository(database, database.routineDao(), database.exerciseDao())
        val routineId = routines.saveRoutine(
            routineOf("Push", newExercise("Squat") to listOf(PlannedSet(5, 100.0), PlannedSet(5, 100.0))),
        )
        val sessionId = workouts.startWorkout(routines.getRoutine(routineId)!!, startedAt)!!
        workouts.finishWorkout(workouts.getWorkout(sessionId)!!.withAllSetsCompleted(), startedAt.plusSeconds(3_600))
        return sessionId
    }

    /** A loaded detail screen, already in edit mode. */
    private suspend fun editingViewModel(sessionId: Int): WorkoutDetailViewModel {
        val factory = viewModelFactory {
            initializer {
                WorkoutDetailViewModel(
                    sessionId = sessionId,
                    workoutRepository = workouts,
                    exerciseRepository = ExerciseRepository(database, database.exerciseDao()),
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
