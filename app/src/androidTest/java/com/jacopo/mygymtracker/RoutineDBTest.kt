package com.jacopo.mygymtracker

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jacopo.mygymtracker.data.db.AppDatabase
import com.jacopo.mygymtracker.data.model.Exercise
import com.jacopo.mygymtracker.data.model.ExerciseDao
import com.jacopo.mygymtracker.data.model.ExerciseSet
import com.jacopo.mygymtracker.data.model.Routine
import com.jacopo.mygymtracker.data.model.RoutineDao
import com.jacopo.mygymtracker.data.model.SetDao
import com.jacopo.mygymtracker.model.Category
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var routineDao: RoutineDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var setDao: SetDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        routineDao = db.routineDao()
        exerciseDao = db.exerciseDao()
        setDao = db.setDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertRoutineWithExercisesAndSets() {
        // Inserisci Routine
        val routine = Routine(id=0, name = "Routine A", note = "Note")
        val routineId = routineDao.insert(routine).toInt()

        // Inserisci Exercise
        val exercise = Exercise(
            name = "Push Up",
            category = Category.CHEST,
            routineId = routineId,
            id = 0 // id fittizio, Room lo autogenera
        )
        val exerciseId = exerciseDao.insert(listOf(exercise)).first().toInt()

        // Inserisci Set
        val set = ExerciseSet(
            numberOfReps = 10,
            weight = 0,
            exerciseId = exerciseId,
            id = 0
        )
        setDao.insert(listOf(set))

        // Recupera la routine completa
        val routineWithAll = routineDao.getRoutineById(routineId)

        assertNotNull(routineWithAll)
        assertEquals(1, routineWithAll!!.exercises.size)
        assertEquals("Push Up", routineWithAll.exercises.first().exercise.name)
        assertEquals(1, routineWithAll.exercises.first().sets.size)
        assertEquals(10, routineWithAll.exercises.first().sets.first().numberOfReps)
    }

    @Test
    fun deleteRoutine_cascadeDeletesExercisesAndSets() {
        // Inserimento
        val routine = Routine(id=0, name = "Delete Test", note = null)
        val routineId = routineDao.insert(routine).toInt()

        val exerciseId = exerciseDao.insert(
            listOf(Exercise(name = "Squat", category = Category.LEGS, routineId = routineId, id = 0))
        ).first().toInt()

        setDao.insert(
            listOf(ExerciseSet(numberOfReps = 12, weight = 20, exerciseId = exerciseId, id = 0))
        )

        // Verifica che tutto sia inserito
        assertEquals(1, routineDao.getRoutineById(routineId)?.exercises?.size)

        // Elimina la routine (che ha onDelete = CASCADE)
        routineDao.delete(Routine(id = routineId, name = "Delete Test", note = null))

        // Verifica che anche esercizi e set siano stati cancellati
        assertTrue(exerciseDao.getAll().isEmpty())
        assertTrue(setDao.getAll().isEmpty())
    }
}
