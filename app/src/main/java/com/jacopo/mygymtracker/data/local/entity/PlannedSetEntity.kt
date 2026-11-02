package com.jacopo.mygymtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A planned set of a [RoutineExerciseEntity], ordered by [position]. */
@Entity(
    tableName = "planned_sets",
    foreignKeys = [
        ForeignKey(
            entity = RoutineExerciseEntity::class,
            parentColumns = ["routineExerciseId"],
            childColumns = ["routineExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routineExerciseId")],
)
data class PlannedSetEntity(
    @PrimaryKey(autoGenerate = true)
    val plannedSetId: Int = 0,
    val routineExerciseId: Int,
    val position: Int,
    val reps: Int,
    val weightKg: Double,
)
