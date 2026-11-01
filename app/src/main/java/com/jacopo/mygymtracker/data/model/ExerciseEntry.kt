package com.jacopo.mygymtracker.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.jacopo.mygymtracker.model.Category

@Entity
data class ExerciseEntry(
    @PrimaryKey(autoGenerate = true)
    val exerciseId: Int,
    val name: String,
    val category: Category,
    val note: String? = null,
    val routineId: Int,
)

data class ExerciseWithSetsEntry(
    @Embedded
    val exerciseEntry: ExerciseEntry,
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "exerciseId",
    )
    val sets: List<SetEntry>
)
