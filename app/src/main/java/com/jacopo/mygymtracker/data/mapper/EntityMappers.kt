package com.jacopo.mygymtracker.data.mapper

import com.jacopo.mygymtracker.data.local.entity.ExerciseEntity
import com.jacopo.mygymtracker.data.local.entity.RoutineEntity
import com.jacopo.mygymtracker.data.local.entity.RoutineWithExercises
import com.jacopo.mygymtracker.data.local.entity.SetEntity
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.ExerciseSet
import com.jacopo.mygymtracker.domain.model.Routine

// region Routine

fun Routine.toEntity(): RoutineEntity = RoutineEntity(
    routineId = id ?: 0,
    name = name,
    note = note,
)

/**
 * Assembles a fully populated [Routine] from its Room relation and the sets that belong to it.
 * Sets are matched to exercises by `exerciseId`.
 */
fun RoutineWithExercises.toDomain(routineSets: List<SetEntity>): Routine {
    val setsByExercise = routineSets.groupBy { it.exerciseId }
    return Routine(
        id = routine.routineId,
        name = routine.name,
        note = routine.note,
        exercises = exercises.map { exercise ->
            exercise.toDomain(sets = setsByExercise[exercise.exerciseId].orEmpty().map { it.toDomain() })
        },
    )
}

// endregion

// region Exercise

fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    exerciseId = id ?: 0,
    name = name,
    category = category,
    note = note,
)

fun ExerciseEntity.toDomain(sets: List<ExerciseSet> = emptyList()): Exercise = Exercise(
    id = exerciseId,
    name = name,
    category = category,
    note = note,
    sets = sets,
)

// endregion

// region Set

fun ExerciseSet.toEntity(exerciseId: Int, routineId: Int): SetEntity = SetEntity(
    setId = id ?: 0,
    exerciseId = exerciseId,
    routineId = routineId,
    weight = weight,
    reps = reps,
)

fun SetEntity.toDomain(): ExerciseSet = ExerciseSet(
    id = setId,
    reps = reps,
    weight = weight,
)

// endregion
