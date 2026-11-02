package com.jacopo.mygymtracker.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room relation: a workout session with its exercises, catalog exercises, and logged sets.
 * [exercises] come back in no particular order; sort them by `position` when mapping.
 */
data class WorkoutSessionWithExercises(
    @Embedded
    val session: WorkoutSessionEntity,
    @Relation(
        entity = WorkoutExerciseEntity::class,
        parentColumn = "sessionId",
        entityColumn = "sessionId",
    )
    val exercises: List<WorkoutExerciseWithDetails>,
)