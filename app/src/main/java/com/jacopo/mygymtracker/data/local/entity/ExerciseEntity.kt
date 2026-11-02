package com.jacopo.mygymtracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jacopo.mygymtracker.domain.model.Category

/**
 * A reusable exercise in the global catalog.
 * It is linked to routines and workouts only through [RoutineExerciseEntity] and
 * [WorkoutExerciseEntity], and must never hold a routine or workout id.
 * Names are unique, ignoring case.
 */
@Entity(
    tableName = "exercises",
    indices = [Index(value = ["name"], unique = true)],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val exerciseId: Int = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val name: String,
    val category: Category,
    val note: String? = null,
)
