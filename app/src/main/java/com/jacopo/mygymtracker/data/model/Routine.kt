package com.jacopo.mygymtracker.data.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.jacopo.mygymtracker.model.Category


/* -------------------- Entities -------------------- */

@Entity
data class Routine(
    @PrimaryKey(autoGenerate = true)
    val id : Int,
    val name: String,
    val note: String? = null,
)

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
    // Foreign key to Routine
    val routineId: Int,
)

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
    val numberOfReps: Int,
    val weight: Int,
    // Foreign key to Exercise
    val exerciseId: Int,
)

/* -------------------- Relationships -------------------- */

data class RoutineWithExercisesWithSets(
    @Embedded
    val routine: Routine,
    @Relation(
        entity = Exercise::class,
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val exercises: List<ExerciseWithSets>
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

/* -------------------- DAOs -------------------- */

@Dao
interface ExerciseDao {
    @Insert
    fun insert(exercise: List<Exercise>) : List<Long>

    @Delete
    fun delete(exercise: List<Exercise>)

    @Transaction
    @Query("SELECT * FROM Exercise")
    fun getAll(): List<ExerciseWithSets>
}

@Dao
interface SetDao {
    @Insert
    fun insert(set: List<ExerciseSet>) : List<Long>

    @Delete
    fun delete(set: List<ExerciseSet>)

    @Transaction
    @Query("SELECT * FROM ExerciseSet")
    fun getAll(): List<ExerciseSet>
}

@Dao
interface RoutineDao {
    @Insert
    fun insert(routine: Routine) : Long
    @Delete
    fun delete(routine: Routine)
    @Transaction
    @Query(
        "SELECT * FROM Routine WHERE id = :id"
    )
    fun getRoutineById(id: Int): RoutineWithExercisesWithSets?
}


