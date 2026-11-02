package com.jacopo.mygymtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jacopo.mygymtracker.domain.model.Category

/**
 * A reusable exercise in the global catalog.
 * It is linked to routines only through [RoutineExerciseCrossRef] and must never hold a routine id.
 */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val exerciseId: Int = 0,
    val name: String,
    val category: Category,
    val note: String? = null,
)
