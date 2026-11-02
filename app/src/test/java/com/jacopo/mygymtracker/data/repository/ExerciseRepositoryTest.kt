package com.jacopo.mygymtracker.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jacopo.mygymtracker.data.createTestDatabase
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.newExercise
import com.jacopo.mygymtracker.data.routineOf
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var exercises: ExerciseRepository
    private lateinit var routines: RoutineRepository
    private lateinit var workouts: WorkoutRepository

    @Before
    fun setUp() {
        database = createTestDatabase()
        exercises = ExerciseRepository(database, database.exerciseDao())
        routines = RoutineRepository(database, database.routineDao(), database.exerciseDao())
        workouts = WorkoutRepository(database, database.workoutDao(), database.exerciseDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun saveExercise_rejectsDuplicateNameIgnoringCase() = runTest {
        assertTrue(exercises.saveExercise(newExercise("Bench Press")))

        assertFalse(exercises.saveExercise(newExercise("  bench press ")))
        assertEquals(1, exercises.observeCatalog().first().size)
    }

    @Test
    fun saveExercise_renameAppliesToRoutines() = runTest {
        val routineId = routines.saveRoutine(routineOf("Push", newExercise("Bench") to emptyList()))
        val bench = catalogEntry("Bench")

        assertTrue(exercises.saveExercise(bench.copy(name = "Bench Press", category = Category.CHEST)))

        val linked = routines.getRoutine(routineId)!!.exercises.single().exercise
        assertEquals(Exercise(bench.id, "Bench Press", Category.CHEST), linked)
    }

    @Test
    fun saveExercise_keepingOwnNameIsNotADuplicate() = runTest {
        exercises.saveExercise(newExercise("Squat"))
        val squat = catalogEntry("Squat")

        assertTrue(exercises.saveExercise(squat.copy(name = "SQUAT", category = Category.LEGS)))
        assertEquals("SQUAT", catalogEntry("SQUAT").name)
    }

    @Test
    fun deleteExercise_onlyWhenUnused() = runTest {
        exercises.saveExercise(newExercise("Unused"))
        routines.saveRoutine(routineOf("Push", newExercise("In routine") to emptyList()))
        val unused = catalogEntry("Unused")
        val inRoutine = catalogEntry("In routine")

        assertFalse(exercises.deleteExercise(inRoutine.id!!))
        assertTrue(exercises.deleteExercise(unused.id!!))
        assertEquals(listOf("In routine"), exercises.observeCatalog().first().map { it.name })
    }

    @Test
    fun deleteExercise_blockedByWorkoutHistoryAfterRoutineIsGone() = runTest {
        val routineId = routines.saveRoutine(routineOf("Push", newExercise("Squat") to emptyList()))
        val workoutId = workouts.startWorkout(routines.getRoutine(routineId)!!)
        routines.deleteRoutine(routineId)
        val squat = catalogEntry("Squat")

        assertTrue(exercises.isInUse(squat.id!!))
        assertFalse(exercises.deleteExercise(squat.id!!))

        workouts.deleteWorkout(workoutId)
        assertTrue(exercises.deleteExercise(squat.id!!))
    }

    private suspend fun catalogEntry(name: String): Exercise =
        exercises.observeCatalog().first().single { it.name == name }
}
