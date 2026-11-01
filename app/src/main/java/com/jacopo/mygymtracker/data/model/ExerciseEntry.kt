package com.jacopo.mygymtracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jacopo.mygymtracker.model.Category

@Entity
data class ExerciseEntry(
    @PrimaryKey(autoGenerate = true)
    val exerciseId: Int = 0,
    val name: String,
    val category: Category,
    val note: String? = null,
)
