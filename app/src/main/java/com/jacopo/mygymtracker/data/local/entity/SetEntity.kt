package com.jacopo.mygymtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A planned set of a catalog exercise, scoped to a specific routine. */
@Entity(
    tableName = "sets",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["routineId"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["exerciseId"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("routineId"), Index("exerciseId")],
)
data class SetEntity(
    @PrimaryKey(autoGenerate = true)
    val setId: Int = 0,
    val exerciseId: Int,
    val routineId: Int,
    val weight: Int,
    val reps: Int,
)
