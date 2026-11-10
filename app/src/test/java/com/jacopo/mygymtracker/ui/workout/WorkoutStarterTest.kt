package com.jacopo.mygymtracker.ui.workout

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jacopo.mygymtracker.data.createTestDatabase
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.repository.WorkoutRepository
import com.jacopo.mygymtracker.domain.model.Routine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutStarterTest {

    private lateinit var database: AppDatabase
    private lateinit var workouts: WorkoutRepository

    @Before
    fun setUp() {
        database = createTestDatabase()
        workouts = WorkoutRepository(database, database.workoutDao(), database.exerciseDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun start_opensTheNewWorkout() = runTest {
        val starter = starter()

        starter.startEmpty("Quick one")

        val sessionId = starter.startedWorkoutId.first { it != null }
        assertEquals(sessionId, workouts.observeActiveWorkout().first()?.id)
        starter.onWorkoutOpened()
        assertNull(starter.startedWorkoutId.value)
    }

    @Test
    fun start_whileAnotherRuns_asksThenDiscardsAndStarts() = runTest {
        val active = workouts.observeActiveWorkout().stateIn(backgroundScope, SharingStarted.Eagerly, null)
        val starter = WorkoutStarter(workouts, this) { active.value }
        starter.startEmpty("First")
        val firstId = starter.startedWorkoutId.first { it != null }
        active.first { it?.id == firstId }
        starter.onWorkoutOpened()

        val second = Routine(name = "Second")
        starter.start(second)
        assertEquals(second, starter.conflict.value)

        starter.discardActiveAndStart()

        assertNull(starter.conflict.value)
        val secondId = starter.startedWorkoutId.first { it != null }
        assertEquals("Second", workouts.getWorkout(checkNotNull(secondId))?.routineName)
        assertNull(workouts.getWorkout(checkNotNull(firstId)))
    }

    @Test
    fun dismissConflict_keepsTheRunningWorkout() = runTest {
        val starter = starter()
        workouts.startWorkout(Routine(name = "Running"))

        // The repository refuses a second workout even when the screen has not seen the first yet.
        starter.startEmpty("Another")
        starter.conflict.first { it != null }
        starter.dismissConflict()

        assertNull(starter.conflict.value)
        assertEquals("Running", workouts.observeActiveWorkout().first()?.routineName)
    }

    /** A screen that has not seen any running workout yet. */
    private fun TestScope.starter() = WorkoutStarter(workouts, this) { null }
}
