package com.jacopo.mygymtracker.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room relation: a workout exercise with its catalog exercise and logged sets.
 * [sets] come back in no particular order; sort them by `position` when mapping.
 */
data class WorkoutExerciseWithDetails(
    @Embedded
    val workoutExercise: WorkoutExerciseEntity,
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "exerciseId",
    )
    val exercise: ExerciseEntity,
    @Relation(
        parentColumn = "workoutExerciseId",
        entityColumn = "workoutExerciseId",
    )
    val sets: List<LoggedSetEntity>,
)