package com.jacopo.mygymtracker.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/** Room relation: a routine together with the catalog exercises linked to it. */
data class RoutineWithExercises(
    @Embedded
    val routine: RoutineEntity,
    @Relation(
        parentColumn = "routineId",
        entityColumn = "exerciseId",
        associateBy = Junction(RoutineExerciseCrossRef::class),
    )
    val exercises: List<ExerciseEntity>,
)
