package com.jacopo.mygymtracker.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity
data class RoutineEntry(
    @PrimaryKey(autoGenerate = true) val routineId: Int,
    val name: String,
    val note: String? = null,
)


@Entity(primaryKeys = ["routineId", "exerciseId"],
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
    ])
data class RoutineExerciseCrossRef(
    val routineId: Int,
    val exerciseId: Int
)


data class RoutineWithExercisesEntry(
    @Embedded val routineEntry: RoutineEntry,
    @Relation(
        parentColumn = "routineId",
        entityColumn = "exerciseId",
        associateBy = Junction(RoutineExerciseCrossRef::class)
    ) val exercises: List<ExerciseEntry>
)









