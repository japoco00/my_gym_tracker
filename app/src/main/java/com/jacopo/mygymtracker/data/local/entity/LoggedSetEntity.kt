package com.jacopo.mygymtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A set performed in a [WorkoutExerciseEntity], ordered by [position]. */
@Entity(
    tableName = "logged_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["workoutExerciseId"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutExerciseId")],
)
data class LoggedSetEntity(
    @PrimaryKey(autoGenerate = true)
    val loggedSetId: Int = 0,
    val workoutExerciseId: Int,
    val position: Int,
    val reps: Int,
    val weightKg: Double,
    val isCompleted: Boolean,
)
