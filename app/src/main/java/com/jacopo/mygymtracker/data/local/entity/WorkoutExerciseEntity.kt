package com.jacopo.mygymtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A catalog [ExerciseEntity] performed inside a [WorkoutSessionEntity] at [position].
 * A catalog exercise cannot be deleted while it appears in a workout.
 * [restSeconds] is copied from the routine; `null` means the default from the settings.
 */
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["exerciseId"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val workoutExerciseId: Int = 0,
    val sessionId: Int,
    val exerciseId: Int,
    val position: Int,
    val restSeconds: Int? = null,
)
