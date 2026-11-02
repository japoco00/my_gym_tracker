package com.jacopo.mygymtracker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jacopo.mygymtracker.data.local.AppDatabase
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.PlannedSet
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.RoutineExercise
import com.jacopo.mygymtracker.domain.model.WorkoutSession

/** Empty in-memory database: no catalog seed, so each test controls its own data. */
fun createTestDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .build()

fun newExercise(name: String, category: Category = Category.OTHER) =
    Exercise(name = name, category = category)

fun routineOf(name: String, vararg exercises: Pair<Exercise, List<PlannedSet>>) = Routine(
    name = name,
    exercises = exercises.map { (exercise, sets) -> RoutineExercise(exercise, sets) },
)

fun WorkoutSession.withAllSetsCompleted(): WorkoutSession = copy(
    exercises = exercises.map { exercise ->
        exercise.copy(sets = exercise.sets.map { it.copy(isCompleted = true) })
    },
)