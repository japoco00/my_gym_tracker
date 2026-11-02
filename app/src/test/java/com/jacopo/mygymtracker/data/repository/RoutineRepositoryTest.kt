package com.jacopo.mygymtracker.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jacopo.mygymtracker.data.createTestDatabase
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.data.mapper.toEntity
import com.jacopo.mygymtracker.data.newExercise
import com.jacopo.mygymtracker.data.routineOf
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.PlannedSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutineRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RoutineRepository

    @Before
    fun setUp() {
        database = createTestDatabase()
        repository = RoutineRepository(database, database.routineDao(), database.exerciseDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun saveRoutine_persistsExerciseAndSetOrder() = runTest {
        val id = repository.saveRoutine(
            routineOf(
                "Push",
                newExercise("Squat") to listOf(PlannedSet(5, 100.0), PlannedSet(3, 110.5)),
                newExercise("Bench Press") to listOf(PlannedSet(8, 60.0)),
                newExercise("Arnold Press") to emptyList(),
            ),
        )

        val routine = repository.getRoutine(id)!!

        assertEquals(listOf("Squat", "Bench Press", "Arnold Press"), routine.exercises.map { it.exercise.name })
        assertEquals(listOf(PlannedSet(5, 100.0), PlannedSet(3, 110.5)), routine.exercises[0].sets)
    }

    @Test
    fun saveRoutine_reusesCatalogExerciseWithSameNameIgnoringCase() = runTest {
        val catalogId = database.exerciseDao()
            .insert(newExercise("Bench Press", Category.CHEST).toEntity()).toInt()

        val id = repository.saveRoutine(routineOf("Push", newExercise("bench press") to emptyList()))

        val exercise = repository.getRoutine(id)!!.exercises.single().exercise
        assertEquals(catalogId, exercise.id)
        assertEquals("Bench Press", exercise.name)
        assertEquals(1, database.exerciseDao().observeAll().first().size)
    }

    @Test
    fun saveRoutine_neverRenamesSavedCatalogExercise() = runTest {
        val id = repository.saveRoutine(routineOf("Push", newExercise("Bench Press") to emptyList()))
        val saved = repository.getRoutine(id)!!
        val renamed = saved.exercises.single().let { it.copy(exercise = it.exercise.copy(name = "Renamed")) }

        repository.saveRoutine(saved.copy(exercises = listOf(renamed)))

        assertEquals("Bench Press", repository.getRoutine(id)!!.exercises.single().exercise.name)
    }

    @Test
    fun saveRoutine_replacesPreviousStructure() = runTest {
        val id = repository.saveRoutine(
            routineOf("Push", newExercise("Squat") to emptyList(), newExercise("Bench Press") to emptyList()),
        )
        val saved = repository.getRoutine(id)!!

        repository.saveRoutine(saved.copy(name = "Push v2", exercises = saved.exercises.reversed().take(1)))

        val updated = repository.observeRoutines().first().single()
        assertEquals("Push v2", updated.name)
        assertEquals(listOf("Bench Press"), updated.exercises.map { it.exercise.name })
    }

    @Test
    fun deleteRoutine_keepsCatalogExercises() = runTest {
        val id = repository.saveRoutine(routineOf("Push", newExercise("Squat") to emptyList()))

        repository.deleteRoutine(id)

        assertNull(repository.getRoutine(id))
        assertEquals(listOf("Squat"), database.exerciseDao().observeAll().first().map { it.name })
    }
}