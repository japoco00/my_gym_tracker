package com.jacopo.mygymtracker.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jacopo.mygymtracker.data.createTestDatabase
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.newExercise
import com.jacopo.mygymtracker.data.routineOf
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.PlannedSet
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.SetType
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var routines: RoutineRepository
    private lateinit var workouts: WorkoutRepository

    @Before
    fun setUp() {
        database = createTestDatabase()
        routines = RoutineRepository(database, database.routineDao(), database.exerciseDao())
        workouts = WorkoutRepository(database, database.workoutDao(), database.exerciseDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun startWorkout_prefillsPlanAndBecomesActive() = runTest {
        val routine = savedRoutine()

        val id = workouts.startWorkout(routine, now = time(1))!!

        val active = workouts.observeActiveWorkout().first()!!
        assertEquals(id, active.id)
        assertEquals("Push", active.routineName)
        assertEquals(
            listOf(LoggedSet(5, 100.0), LoggedSet(5, 100.0)),
            active.exercises.first().sets,
        )
    }

    @Test
    fun startWorkout_returnsNullWhileAnotherIsActive() = runTest {
        val routine = savedRoutine()
        val firstId = workouts.startWorkout(routine)

        assertNull(workouts.startWorkout(routine))
        assertEquals(firstId, workouts.observeActiveWorkout().first()?.id)
    }

    @Test
    fun restSeconds_comesFromRoutineAndSurvivesDraftAndFinish() = runTest {
        val routine = savedRoutine().let { it.copy(exercises = listOf(it.exercises[0].copy(restSeconds = 180))) }
        val id = workouts.startWorkout(routine)!!
        assertEquals(180, workouts.getWorkout(id)!!.exercises.single().restSeconds)

        val draft = workouts.getWorkout(id)!!.completeFirstSetOfFirstExercise()
        workouts.saveDraft(draft.copy(exercises = draft.exercises.map { it.copy(restSeconds = 120) }))
        val finished = workouts.finishWorkout(workouts.getWorkout(id)!!)!!

        assertEquals(120, finished.exercises.single().restSeconds)
    }

    @Test
    fun finishWorkout_keepsOnlyCompletedSets() = runTest {
        val id = workouts.startWorkout(savedRoutine(), now = time(1))!!
        val draft = workouts.getWorkout(id)!!.completeFirstSetOfFirstExercise()

        val finished = workouts.finishWorkout(draft, now = time(2))!!

        assertNull(workouts.observeActiveWorkout().first())
        assertEquals(time(2), finished.finishedAt)
        assertEquals(listOf("Squat"), finished.exercises.map { it.exercise.name })
        assertEquals(listOf(LoggedSet(5, 100.0, isCompleted = true)), finished.exercises.single().sets)
    }

    @Test
    fun finishWorkout_returnsNullOnceWorkoutIsDiscarded() = runTest {
        val id = workouts.startWorkout(savedRoutine())!!
        val draft = workouts.getWorkout(id)!!.completeFirstSetOfFirstExercise()
        workouts.deleteWorkout(id)

        assertNull(workouts.finishWorkout(draft))
        assertNull(workouts.getWorkout(id))
    }

    @Test
    fun updateFinishedWorkout_replacesSetsAndKeepsTimes() = runTest {
        val sessionId = finishWorkout(savedRoutine(), at = 1)
        val squat = workouts.getWorkout(sessionId)!!.exercises.single()
        val edits = listOf(
            squat.copy(sets = listOf(LoggedSet(6, 102.5), LoggedSet(4, 102.5))),
            squat.copy(exercise = newExercise("Curl"), sets = emptyList()),
        )

        val updated = workouts.updateFinishedWorkout(sessionId, edits)!!

        assertEquals(time(1), updated.startedAt)
        assertEquals(time(2), updated.finishedAt)
        assertEquals(listOf("Squat"), updated.exercises.map { it.exercise.name })
        assertEquals(
            listOf(LoggedSet(6, 102.5, isCompleted = true), LoggedSet(4, 102.5, isCompleted = true)),
            updated.exercises.single().sets,
        )
    }

    @Test
    fun updateFinishedWorkout_returnsNullForWorkoutInProgress() = runTest {
        val id = workouts.startWorkout(savedRoutine())!!
        val draft = workouts.getWorkout(id)!!

        assertNull(workouts.updateFinishedWorkout(id, draft.exercises))
        assertEquals(draft, workouts.getWorkout(id))
    }

    @Test
    fun history_aggregatesCompletedSetsNewestFirst() = runTest {
        val routine = savedRoutine()
        finishWorkout(routine, at = 1)
        val secondId = finishWorkout(routine, at = 10)

        val history = workouts.observeHistory().first()

        assertEquals(2, history.size)
        assertEquals(secondId, history.first().id)
        assertEquals(1, history.first().setCount)
        assertEquals(500.0, history.first().volumeKg, 1e-9)
    }

    @Test
    fun history_leavesWarmUpsOutOfVolume() = runTest {
        val id = workouts.startWorkout(savedRoutine(), now = time(0))!!
        val draft = workouts.getWorkout(id)!!.completeFirstSetOfFirstExercise()
        val first = draft.exercises.first()
        val warmUp = LoggedSet(reps = 5, weightKg = 60.0, isCompleted = true, type = SetType.WARM_UP, rpe = 6.5)
        val withWarmUp = listOf(first.copy(sets = listOf(warmUp) + first.sets)) + draft.exercises.drop(1)
        workouts.finishWorkout(draft.copy(exercises = withWarmUp), now = time(1))

        assertEquals(warmUp, workouts.getWorkout(id)!!.exercises.first().sets.first())
        val summary = workouts.observeHistory().first().single()
        assertEquals(2, summary.setCount)
        assertEquals(500.0, summary.volumeKg, 1e-9)
    }

    @Test
    fun getLastPerformance_returnsSetsOfLatestFinishedWorkout() = runTest {
        val routine = savedRoutine()
        finishWorkout(routine, at = 1, weightKg = 100.0)
        finishWorkout(routine, at = 10, weightKg = 105.0)
        val squatId = routine.exercises.first().exercise.id!!
        val benchId = routine.exercises.last().exercise.id!!

        val last = workouts.getLastPerformance(listOf(squatId, benchId))

        assertEquals(mapOf(squatId to listOf(LoggedSet(5, 105.0, isCompleted = true))), last)
    }

    @Test
    fun observeExerciseHistory_groupsSetsPerWorkoutOldestFirst() = runTest {
        val routine = savedRoutine()
        finishWorkout(routine, at = 10, weightKg = 105.0)
        finishWorkout(routine, at = 1, weightKg = 100.0)

        val history = workouts.observeExerciseHistory(routine.exercises.first().exercise.id!!).first()

        assertEquals(listOf(time(1), time(10)), history.map { it.performedAt })
        assertEquals(listOf(100.0, 105.0), history.map { it.sets.single().weightKg })
    }

    @Test
    fun saveDraft_isIgnoredOnceWorkoutIsDiscarded() = runTest {
        val id = workouts.startWorkout(savedRoutine())!!
        val draft = workouts.getWorkout(id)!!
        workouts.deleteWorkout(id)

        workouts.saveDraft(draft.completeFirstSetOfFirstExercise())

        assertNull(workouts.getWorkout(id))
    }

    @Test
    fun deletingRoutine_keepsHistoryWithNameSnapshot() = runTest {
        val routine = savedRoutine()
        val sessionId = finishWorkout(routine, at = 1)

        routines.deleteRoutine(routine.id!!)

        val workout = workouts.getWorkout(sessionId)!!
        assertNull(workout.routineId)
        assertEquals("Push", workout.routineName)
    }

    private suspend fun savedRoutine(): Routine {
        val id = routines.saveRoutine(
            routineOf(
                "Push",
                newExercise("Squat") to listOf(PlannedSet(5, 100.0), PlannedSet(5, 100.0)),
                newExercise("Bench Press") to listOf(PlannedSet(8, 60.0)),
            ),
        )
        return routines.getRoutine(id)!!
    }

    /** Starts and finishes a workout where only the first squat set is completed. */
    private suspend fun finishWorkout(routine: Routine, at: Long, weightKg: Double = 100.0): Int {
        val id = workouts.startWorkout(routine, now = time(at))!!
        val draft = workouts.getWorkout(id)!!.completeFirstSetOfFirstExercise(weightKg)
        workouts.finishWorkout(draft, now = time(at + 1))
        return id
    }

    private fun WorkoutSession.completeFirstSetOfFirstExercise(weightKg: Double = 100.0): WorkoutSession {
        val first = exercises.first()
        val sets = first.sets.mapIndexed { index, set ->
            if (index == 0) set.copy(weightKg = weightKg, isCompleted = true) else set
        }
        return copy(exercises = listOf(first.copy(sets = sets)) + exercises.drop(1))
    }

    private fun time(minutes: Long): Instant = Instant.EPOCH.plusSeconds(minutes * 60)
}