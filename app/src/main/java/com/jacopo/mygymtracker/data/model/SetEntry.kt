package com.jacopo.mygymtracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntry::class,
            parentColumns = ["routineId"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntry::class,
            parentColumns = ["exerciseId"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId"), Index("exerciseId")]
)
data class SetEntry(
    @PrimaryKey(autoGenerate = true)
    val setId: Int,
    // Foreign key to Exercise
    val exerciseId: Int,
    // Foreign key to Routine
    val routineId: Int,
    val weight: Int,
    val reps: Int,
)




