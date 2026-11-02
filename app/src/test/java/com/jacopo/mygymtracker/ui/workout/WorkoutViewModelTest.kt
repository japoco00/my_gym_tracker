package com.jacopo.mygymtracker.ui.workout

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
import com.jacopo.mygymtracker.resttimer.RestTimerController
import com.jacopo.mygymtracker.ui.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: AppDatabase
    private lateinit var routines: RoutineRepository
    private lateinit var workouts: WorkoutRepository

    /** Clearing it plays the screen going away: cancels `viewModelScope`, then calls `onCleared`. */
    private val viewModels = ViewModelStore()

    @Before
    fun setUp() {
        database = createTestDatabase()
        routines = RoutineRepository(database, database.routineDao(), database.exerciseDao())
        workouts = WorkoutRepository(database, database.workoutDao(), database.exerciseDao())
    }

    @After
    fun tearDown() {
        viewModels.clear()
        database.close()
    }

    @Test
    fun finishedWorkout_closesScreen() = runTest {
        val sessionId = startWorkout()
        workouts.finishWorkout(workouts.getWorkout(sessionId)!!.withAllSetsCompleted())

        val state = workoutViewModel(sessionId).uiState.first { !it.isLoading }

        assertEquals(WorkoutExit.CLOSED, state.exit)
    }

    @Test
    fun requestFinish_picksDialogFromCompletedSets() = runTest {
        val viewModel = loadedViewModel(startWorkout())

        viewModel.requestFinish()
        assertEquals(WorkoutDialog.NothingCompleted, viewModel.uiState.value.dialog)

        viewModel.completeFirstSet()
        viewModel.requestFinish()
        assertEquals(WorkoutDialog.ConfirmFinish(uncheckedSets = 1), viewModel.uiState.value.dialog)
    }

    @Test
    fun confirmFinish_withUnchangedPlan_finishesWithoutAskingAboutRoutine() = runTest {
        val sessionId = startWorkout()
        val viewModel = loadedViewModel(sessionId)
        viewModel.completeFirstSet()

        viewModel.confirmFinish()

        val state = viewModel.uiState.first { it.exit != null }
        assertEquals(WorkoutExit.FINISHED, state.exit)
        assertNull(state.dialog)
        assertNotNull(workouts.getWorkout(sessionId)!!.finishedAt)
    }

    @Test
    fun confirmFinish_withChangedPlan_updatesRoutineOnRequest() = runTest {
        val viewModel = loadedViewModel(startWorkout())
        viewModel.updateFirstSetReps(6)
        viewModel.completeFirstSet()

        viewModel.confirmFinish()
        viewModel.uiState.first { it.dialog == WorkoutDialog.UpdateRoutine }
        viewModel.resolveRoutineUpdate(update = true)

        assertEquals(WorkoutExit.FINISHED, viewModel.uiState.value.exit)
        val plan = routines.observeRoutines()
            .map { it.single().exercises.single().sets }
            .first { it.first().reps == 6 }
        assertEquals(listOf(PlannedSet(6, 100.0), PlannedSet(5, 100.0)), plan)
    }

    @Test
    fun confirmDiscard_deletesWorkoutAndCloses() = runTest {
        val sessionId = startWorkout()
        val viewModel = loadedViewModel(sessionId)

        viewModel.requestDiscard()
        viewModel.confirmDiscard()

        assertEquals(WorkoutExit.CLOSED, viewModel.uiState.first { it.exit != null }.exit)
        assertNull(workouts.getWorkout(sessionId))
    }

    @Test
    fun edits_areSavedAsDraft() = runTest {
        val viewModel = loadedViewModel(startWorkout())

        viewModel.updateFirstSetReps(8)

        assertEquals(listOf(LoggedSet(8, 100.0), LoggedSet(5, 100.0)), awaitActiveSets { it.first().reps == 8 })
    }

    @Test
    fun closingScreen_savesPendingEdits() = runTest {
        val viewModel = loadedViewModel(startWorkout())
        viewModel.updateFirstSetReps(8)

        // Before the draft delay is over, so only onCleared can save the edit.
        viewModels.clear()

        assertEquals(listOf(LoggedSet(8, 100.0), LoggedSet(5, 100.0)), awaitActiveSets { it.first().reps == 8 })
    }

    /** Starts a workout from a "Push" routine planning two squat sets of 5 x 100 kg. */
    private suspend fun startWorkout(): Int {
        val routineId = routines.saveRoutine(
            routineOf("Push", newExercise("Squat") to listOf(PlannedSet(5, 100.0), PlannedSet(5, 100.0))),
        )
        return workouts.startWorkout(routines.getRoutine(routineId)!!)!!
    }

    private fun TestScope.workoutViewModel(sessionId: Int): WorkoutViewModel {
        val factory = viewModelFactory {
            initializer {
                WorkoutViewModel(
                    sessionId = sessionId,
                    workoutRepository = workouts,
                    routineRepository = routines,
                    exerciseRepository = ExerciseRepository(database, database.exerciseDao()),
                    settingsRepository = SettingsRepository(InMemoryPreferencesDataStore()),
                    restTimer = RestTimerController(backgroundScope, clock = { testScheduler.currentTime }),
                    applicationScope = backgroundScope,
                )
            }
        }
        return ViewModelProvider.create(viewModels, factory)[WorkoutViewModel::class]
    }

    private suspend fun TestScope.loadedViewModel(sessionId: Int): WorkoutViewModel =
        workoutViewModel(sessionId).also { viewModel -> viewModel.uiState.first { !it.isLoading } }

    /** Room writes on its own threads, so wait for the database to show the expected sets. */
    private suspend fun awaitActiveSets(predicate: (List<LoggedSet>) -> Boolean): List<LoggedSet> =
        workouts.observeActiveWorkout()
            .map { it?.exercises?.single()?.sets.orEmpty() }
            .first { it.isNotEmpty() && predicate(it) }

    private fun WorkoutViewModel.updateFirstSetReps(reps: Int) {
        val squat = uiState.value.exercises.single()
        updateSetReps(squat.key, squat.sets.first().key, reps)
    }

    private fun WorkoutViewModel.completeFirstSet() {
        val squat = uiState.value.exercises.single()
        setCompleted(squat.key, squat.sets.first().key, completed = true)
    }
}
