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
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val category: Category,
    val note: String? = null,
    // Foreign key to Routine
    val routineId: Int,
)


data class ExerciseWithSets(
    @Embedded
    val exercise: Exercise,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId"
    )
    val sets: List<ExerciseSet>
)
