package com.jacopo.mygymtracker.ui.routines

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
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
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.ui.MainDispatcherRule
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
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

    /** Each ViewModel's `viewModelScope` job, so [tearDown] can wait for saves that outlive cancelling it. */
    private val viewModelJobs = mutableListOf<Job>()

    @Before
    fun setUp() {
        database = createTestDatabase()
        routines = RoutineRepository(database, database.routineDao(), database.exerciseDao())
        exercises = ExerciseRepository(database, database.exerciseDao())
    }

    @After
    fun tearDown() {
        viewModels.clear()
        // A save already running is NonCancellable on purpose. Closing the database under its
        // transaction breaks it, and the error fails whichever test runs next.
        runBlocking { viewModelJobs.joinAll() }
        database.close()
    }

    @Test
    fun edits_areSavedAutomatically() = runTest {
        val viewModel = loadedViewModel()
        viewModel.updateRoutineName("Legs")
        awaitRoutines { it.isNotEmpty() }
        viewModel.openNewExerciseSheet()

        viewModel.addNewExercise(" Pistol Squat ", Category.LEGS, TrackingType.REPS)

        // single(): the second save updates the routine inserted by the first.
        val saved = awaitRoutines { routines -> routines.any { it.exercises.isNotEmpty() } }.single()
        assertFalse(viewModel.uiState.value.isNewExerciseSheetOpen)
        val exercise = saved.exercises.single()
        assertEquals("Legs", saved.name)
        assertEquals("Pistol Squat", exercise.exercise.name)
        assertEquals(TrackingType.REPS, exercise.exercise.trackingType)
        assertEquals(3, exercise.sets.size)
        val catalog = exercises.observeCatalog().first()
        assertEquals(listOf("Pistol Squat"), catalog.map { it.name })

        // Picking it again from the catalog doesn't add it twice.
        viewModel.addCatalogExercises(catalog)
        assertEquals(1, viewModel.uiState.value.exercises.size)
    }

    @Test
    fun closingScreen_savesPendingEdits() = runTest {
        val viewModel = loadedViewModel()
        viewModel.updateRoutineName("Legs")

        // Before the save delay is over, so only onCleared can save the edit.
        viewModels.clear()

        assertEquals("Legs", awaitRoutines { it.isNotEmpty() }.single().name)
    }

    @Test
    fun newRoutine_clearedBackToBlank_isDeleted() = runTest {
        val viewModel = loadedViewModel()
        viewModel.updateRoutineName("Legs")
        awaitRoutines { it.isNotEmpty() }

        viewModel.updateRoutineName(" ")

        awaitRoutines { it.isEmpty() }
    }

    @Test
    fun setRest_isSavedPerExerciseAndClearsBackToDefault() = runTest {
        val viewModel = loadedViewModel()
        viewModel.addNewExercise("Squat", Category.LEGS, TrackingType.WEIGHT_REPS)
        val squat = viewModel.uiState.value.exercises.single()

        viewModel.setRest(squat.key, 180)
        val saved = awaitRoutines { routines -> routines.any { routine -> routine.exercises.any { it.restSeconds != null } } }
        assertEquals(180, saved.single().exercises.single().restSeconds)

        viewModel.setRest(squat.key, null)
        awaitRoutines { it.single().exercises.single().restSeconds == null }
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

    private suspend fun TestScope.loadedViewModel(): RoutineDetailViewModel {
        val factory = viewModelFactory {
            initializer {
                RoutineDetailViewModel(
                    routineId = null,
                    untitledName = "Untitled",
                    routineRepository = routines,
                    exerciseRepository = exercises,
                    settingsRepository = SettingsRepository(InMemoryPreferencesDataStore()),
                    applicationScope = backgroundScope,
                ).also { viewModel -> viewModelJobs += viewModel.viewModelScope.coroutineContext.job }
            }
        }
        val viewModel = ViewModelProvider.create(viewModels, factory)[RoutineDetailViewModel::class]
        viewModel.uiState.first { !it.isLoading }
        return viewModel
    }

    /** Room writes on its own threads, so wait for the database to show the expected routines. */
    private suspend fun awaitRoutines(predicate: (List<Routine>) -> Boolean): List<Routine> =
        routines.observeRoutines().first(predicate)
}
