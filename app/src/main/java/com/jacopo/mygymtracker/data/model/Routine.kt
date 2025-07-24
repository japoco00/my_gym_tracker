package com.jacopo.mygymtracker.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation


@Entity
data class Routine(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val note: String? = null,
)


data class RoutineWithExercisesWithSets(
    @Embedded
    val routine: Routine,
    @Relation(
        entity = Exercise::class,
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val exercises: List<ExerciseWithSets>
)









