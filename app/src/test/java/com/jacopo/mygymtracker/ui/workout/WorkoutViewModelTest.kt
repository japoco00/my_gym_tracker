package com.jacopo.mygymtracker.ui.workout

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
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
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.resttimer.RestTimerController
import com.jacopo.mygymtracker.ui.MainDispatcherRule
import com.jacopo.mygymtracker.ui.components.EditingSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
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
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class WorkoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: AppDatabase
    private lateinit var routines: RoutineRepository
    private lateinit var workouts: WorkoutRepository

    /** Clearing it plays the screen going away: cancels `viewModelScope`, then calls `onCleared`. */
    private val viewModels = ViewModelStore()

    /** Each ViewModel's `viewModelScope` job, so [tearDown] can wait for saves that outlive cancelling it. */
    private val viewModelJobs = mutableListOf<Job>()

    @Before
    fun setUp() {
        database = createTestDatabase()
        routines = RoutineRepository(database, database.routineDao(), database.exerciseDao())
        workouts = WorkoutRepository(database, database.workoutDao(), database.exerciseDao())
    }

    @After
    fun tearDown() {
        viewModels.clear()
        // A draft save already running is NonCancellable on purpose. Closing the database under its
        // transaction breaks it, and the error fails whichever test runs next.
        runBlocking { viewModelJobs.joinAll() }
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

    @Test
    fun setEditor_opensAndCloses() = runTest {
        val viewModel = loadedViewModel(startWorkout())
        val squat = viewModel.uiState.value.exercises.single()
        val firstSet = squat.sets.first()

        viewModel.openSetEditor(squat.key, firstSet.key)
        assertEquals(EditingSet(squat.key, firstSet.key), viewModel.uiState.value.editingSet)

        viewModel.closeSetEditor()
        assertNull(viewModel.uiState.value.editingSet)
    }

    @Test
    fun removingEditedSet_closesEditor() = runTest {
        val viewModel = loadedViewModel(startWorkout())
        val squat = viewModel.uiState.value.exercises.single()
        val (firstSet, secondSet) = squat.sets

        viewModel.openSetEditor(squat.key, firstSet.key)
        viewModel.removeSet(squat.key, secondSet.key)
        assertEquals(EditingSet(squat.key, firstSet.key), viewModel.uiState.value.editingSet)

        viewModel.removeSet(squat.key, firstSet.key)
        assertNull(viewModel.uiState.value.editingSet)
    }

    @Test
    fun removingEditedExercise_closesEditor() = runTest {
        val viewModel = loadedViewModel(startWorkout())
        val squat = viewModel.uiState.value.exercises.single()

        viewModel.openSetEditor(squat.key, squat.sets.first().key)
        viewModel.confirmRemoveExercise(squat.key)

        assertNull(viewModel.uiState.value.editingSet)
    }

    @Test
    fun totals_countOnlyCompletedSets() = runTest {
        val viewModel = loadedViewModel(startWorkout())

        viewModel.completeFirstSet()

        val state = viewModel.uiState.value
        assertEquals(2, state.totalSets)
        assertEquals(1, state.completedSets)
        assertEquals(500.0, state.completedVolumeKg, 0.0)
    }

    @Test
    fun completingSet_aboveEarlierBest_marksItAsPr() = runTest {
        val push = pushRoutine()
        val earlierId = workouts.startWorkout(push, now = Instant.EPOCH)!!
        workouts.finishWorkout(
            workouts.getWorkout(earlierId)!!.withAllSetsCompleted(),
            now = Instant.EPOCH.plusSeconds(3600),
        )
        val viewModel = loadedViewModel(workouts.startWorkout(push)!!)
        val squat = viewModel.uiState.value.exercises.single()
        val firstSet = squat.sets.first()

        viewModel.completeFirstSet() // 5 x 100 kg only ties the earlier best
        assertEquals(emptySet<String>(), viewModel.uiState.value.prSetKeys)

        viewModel.updateSetWeight(squat.key, firstSet.key, 105.0)
        assertEquals(setOf(firstSet.key), viewModel.uiState.value.prSetKeys)

        viewModel.setCompleted(squat.key, firstSet.key, completed = false)
        assertEquals(emptySet<String>(), viewModel.uiState.value.prSetKeys)
    }

    /** Starts a workout from a "Push" routine planning two squat sets of 5 x 100 kg. */
    private suspend fun startWorkout(): Int = workouts.startWorkout(pushRoutine())!!

    /** Saves a "Push" routine planning two squat sets of 5 x 100 kg. */
    private suspend fun pushRoutine(): Routine {
        val routineId = routines.saveRoutine(
            routineOf("Push", newExercise("Squat") to listOf(PlannedSet(5, 100.0), PlannedSet(5, 100.0))),
        )
        return routines.getRoutine(routineId)!!
    }

    @OptIn(ExperimentalCoroutinesApi::class) // testScheduler.currentTime
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
                ).also { viewModel -> viewModelJobs += viewModel.viewModelScope.coroutineContext.job }
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
