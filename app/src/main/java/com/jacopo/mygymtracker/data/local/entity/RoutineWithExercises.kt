package com.jacopo.mygymtracker.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room relation: a routine with its exercise links, catalog exercises, and planned sets.
 * [exercises] come back in no particular order; sort them by `position` when mapping.
 */
data class RoutineWithExercises(
    @Embedded
    val routine: RoutineEntity,
    @Relation(
        entity = RoutineExerciseEntity::class,
        parentColumn = "routineId",
        entityColumn = "routineId",
    )
    val exercises: List<RoutineExerciseWithDetails>,
)
