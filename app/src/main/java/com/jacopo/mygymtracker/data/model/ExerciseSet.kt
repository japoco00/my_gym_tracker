package com.jacopo.mygymtracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
)
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val reps: Int,
    val weight: Int,
    // Foreign key to Exercise
    val exerciseId: Int,
)

