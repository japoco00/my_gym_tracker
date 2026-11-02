package com.jacopo.mygymtracker.data.mapper

import com.jacopo.mygymtracker.data.local.entity.ExerciseEntity
import com.jacopo.mygymtracker.data.local.entity.PlannedSetEntity
import com.jacopo.mygymtracker.data.local.entity.RoutineEntity
import com.jacopo.mygymtracker.data.local.entity.RoutineExerciseWithDetails
import com.jacopo.mygymtracker.data.local.entity.RoutineWithExercises
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.PlannedSet
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.RoutineExercise

// region Exercise

fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    exerciseId = id ?: 0,
    name = name,
    category = category,
    note = note,
)

fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = exerciseId,
    name = name,
    category = category,
    note = note,
)

// endregion

// region Routine

fun Routine.toEntity(): RoutineEntity = RoutineEntity(
    routineId = id ?: 0,
    name = name,
    note = note,
)

fun RoutineWithExercises.toDomain(): Routine = Routine(
    id = routine.routineId,
    name = routine.name,
    note = routine.note,
    exercises = exercises
        .sortedBy { it.routineExercise.position }
        .map { it.toDomain() },
)

fun RoutineExerciseWithDetails.toDomain(): RoutineExercise = RoutineExercise(
    exercise = exercise.toDomain(),
    sets = sets.sortedBy { it.position }.map { it.toDomain() },
)

fun PlannedSet.toEntity(routineExerciseId: Int, position: Int): PlannedSetEntity = PlannedSetEntity(
    routineExerciseId = routineExerciseId,
    position = position,
    reps = reps,
    weightKg = weightKg,
)

fun PlannedSetEntity.toDomain(): PlannedSet = PlannedSet(
    reps = reps,
    weightKg = weightKg,
)

// endregion