package com.jacopo.mygymtracker.ui.home

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jacopo.mygymtracker.data.InMemoryPreferencesDataStore
import com.jacopo.mygymtracker.data.createTestDatabase
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.newExercise
import com.jacopo.mygymtracker.data.repository.BodyWeightRepository
import com.jacopo.mygymtracker.data.repository.RoutineRepository
import com.jacopo.mygymtracker.data.repository.SettingsRepository
import com.jacopo.mygymtracker.data.repository.WorkoutRepository
import com.jacopo.mygymtracker.data.routineOf
import com.jacopo.mygymtracker.domain.home.Greeting
import com.jacopo.mygymtracker.domain.home.HomeSituation
import com.jacopo.mygymtracker.domain.model.BodyWeight
import com.jacopo.mygymtracker.domain.model.PlannedSet
import com.jacopo.mygymtracker.domain.model.Routine
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
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: AppDatabase
    private lateinit var routines: RoutineRepository
    private lateinit var workouts: WorkoutRepository
    private lateinit var bodyWeights: BodyWeightRepository
    private val settings = SettingsRepository(InMemoryPreferencesDataStore())
    private val viewModels = ViewModelStore()

    // Wednesday 2025-10-15 at 19:00: an evening.
    private val now = LocalDateTime.of(2025, 10, 15, 19, 0).toInstant(ZoneOffset.UTC)

    @Before
    fun setUp() {
        database = createTestDatabase()
        routines = RoutineRepository(database, database.routineDao(), database.exerciseDao())
        workouts = WorkoutRepository(database, database.workoutDao(), database.exerciseDao())
        bodyWeights = BodyWeightRepository(database, database.bodyWeightDao())
    }

    @After
    fun tearDown() {
        viewModels.clear()
        database.close()
    }

    @Test
    fun newUser_isWelcomedByNameWithNothingToShow() = runTest {
        settings.setName("Jacopo")

        val state = loadedViewModel(seed = 0).uiState.first { !it.isLoading }

        assertEquals(HomeSituation.New, state.situation)
        assertEquals(Greeting.Welcome("Jacopo"), state.greeting)
        assertNull(state.upNext)
        assertNull(state.stats)
    }

    @Test
    fun history_givesUpNextStatsAndRecords() = runTest {
        val push = savedRoutine("Push")
        val pull = savedRoutine("Pull")
        finishWorkout(push, at = now - Duration.ofDays(3), weightKg = 100.0)
        finishWorkout(push, at = now - Duration.ofDays(2), weightKg = 110.0)

        val state = loadedViewModel().uiState.first { !it.isLoading }

        assertEquals(HomeSituation.Usual, state.situation)
        assertEquals(pull.id, state.upNext?.id)
        assertEquals(2, state.stats?.totalWorkouts)
        assertEquals(listOf(110.0), state.records.map { it.set.weightKg })
    }

    @Test
    fun greeting_followsTheSituationButKeepsItsSeed() = runTest {
        val push = savedRoutine("Push")
        val viewModel = loadedViewModel(seed = 1)
        assertEquals(Greeting.Calling("Push"), viewModel.uiState.first { !it.isLoading }.greeting)

        finishWorkout(push, at = now - Duration.ofHours(1))
        assertEquals(Greeting.NiceWork, viewModel.uiState.first { it.situation == HomeSituation.TrainedToday }.greeting)

        bodyWeights.save(BodyWeight(now.atZone(ZoneOffset.UTC).toLocalDate(), 80.0))
        assertEquals(Greeting.NiceWork, viewModel.uiState.first { it.latestBodyWeight != null }.greeting)
    }

    @Test
    fun startWorkout_makesItTheActiveOne() = runTest {
        val push = savedRoutine("Push")
        val viewModel = loadedViewModel()

        viewModel.startWorkout(push)

        val state = viewModel.uiState.first { it.startedWorkoutId != null && it.activeWorkout != null }
        assertEquals(state.startedWorkoutId, state.activeWorkout?.id)
    }

    private suspend fun loadedViewModel(seed: Int = 0): HomeViewModel {
        val factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    routineRepository = routines,
                    workoutRepository = workouts,
                    settingsRepository = settings,
                    bodyWeightRepository = bodyWeights,
                    clock = Clock.fixed(now, ZoneOffset.UTC),
                    firstDayOfWeek = DayOfWeek.MONDAY,
                    seed = seed,
                )
            }
        }
        val viewModel = ViewModelProvider.create(viewModels, factory)[HomeViewModel::class]
        viewModel.uiState.first { !it.isLoading }
        return viewModel
    }

    private suspend fun savedRoutine(name: String): Routine {
        val id = routines.saveRoutine(routineOf(name, newExercise("Squat") to listOf(PlannedSet(5, 100.0))))
        return routines.getRoutine(id)!!
    }

    private suspend fun finishWorkout(routine: Routine, at: Instant, weightKg: Double = 100.0) {
        val id = workouts.startWorkout(routine, now = at)!!
        val draft = workouts.getWorkout(id)!!
        val exercise = draft.exercises.first()
        val done = exercise.copy(sets = exercise.sets.map { it.copy(weightKg = weightKg, isCompleted = true) })
        workouts.finishWorkout(draft.copy(exercises = listOf(done)), now = at + Duration.ofHours(1))
    }
}
