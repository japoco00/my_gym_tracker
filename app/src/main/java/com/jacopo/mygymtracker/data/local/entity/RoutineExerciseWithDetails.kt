package com.jacopo.mygymtracker.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room relation: a routine exercise link with its catalog exercise and planned sets.
 * [sets] come back in no particular order; sort them by `position` when mapping.
 */
data class RoutineExerciseWithDetails(
    @Embedded
    val routineExercise: RoutineExerciseEntity,
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "exerciseId",
    )
    val exercise: ExerciseEntity,
    @Relation(
        parentColumn = "routineExerciseId",
        entityColumn = "routineExerciseId",
    )
    val sets: List<PlannedSetEntity>,
)
