package com.jacopo.mygymtracker.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity
data class RoutineEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val note: String? = null,
)

data class RoutineFullEntry(
    @Embedded
    val routineEntry: RoutineEntry,
    @Relation(
        entity = ExerciseEntry::class,
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val exercises: List<ExerciseWithSetsEntry>
)









