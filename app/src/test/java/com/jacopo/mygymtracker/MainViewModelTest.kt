package com.jacopo.mygymtracker

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
import com.jacopo.mygymtracker.domain.model.PlannedSet
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.ui.MainDispatcherRule
import com.jacopo.mygymtracker.widget.WidgetDestination
import com.jacopo.mygymtracker.widget.WidgetRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: AppDatabase
    private lateinit var routines: RoutineRepository
    private lateinit var workouts: WorkoutRepository
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
    fun startRequest_startsTheRoutineAndOpensItsWorkout() = runTest {
        val push = savedRoutine("Push")
        val viewModel = viewModel()

        viewModel.onWidgetRequest(WidgetRequest.Start(push.id!!))

        val destination = viewModel.widgetDestination.first { it != null }
        val active = workouts.observeActiveWorkout().first()
        assertEquals(push.id, active?.routineId)
        assertEquals(WidgetDestination.Workout(active?.id!!), destination)
    }

    @Test
    fun startRequest_whileAnotherWorkoutRuns_opensTheRunningOne() = runTest {
        val running = workouts.startWorkout(savedRoutine("Push"))!!
        val pull = savedRoutine("Pull")
        val viewModel = viewModel()

        viewModel.onWidgetRequest(WidgetRequest.Start(pull.id!!))

        assertEquals(WidgetDestination.Workout(running), viewModel.widgetDestination.first { it != null })
        assertEquals(running, workouts.observeActiveWorkout().first()?.id)
    }

    private fun viewModel(): MainViewModel {
        val factory = viewModelFactory {
            initializer { MainViewModel(SettingsRepository(InMemoryPreferencesDataStore()), routines, workouts) }
        }
        return ViewModelProvider.create(viewModels, factory)[MainViewModel::class]
    }

    private suspend fun savedRoutine(name: String): Routine {
        val id = routines.saveRoutine(routineOf(name, newExercise("Squat") to listOf(PlannedSet(5, 100.0))))
        return routines.getRoutine(id)!!
    }
}
