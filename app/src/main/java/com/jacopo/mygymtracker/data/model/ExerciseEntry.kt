package com.jacopo.mygymtracker.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.jacopo.mygymtracker.model.Category

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntry::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExerciseEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val category: Category,
    val note: String? = null,
    // Foreign key to Routine
    val routineId: Int,
)

data class ExerciseWithSetsEntry(
    @Embedded
    val exerciseEntry: ExerciseEntry,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId"
    )
    val sets: List<SetEntry>
)
