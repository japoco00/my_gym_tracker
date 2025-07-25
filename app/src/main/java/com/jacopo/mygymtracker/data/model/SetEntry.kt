package com.jacopo.mygymtracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntry::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
)
data class SetEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val reps: Int,
    val weight: Int,
    // Foreign key to Exercise
    val exerciseId: Int,
)


