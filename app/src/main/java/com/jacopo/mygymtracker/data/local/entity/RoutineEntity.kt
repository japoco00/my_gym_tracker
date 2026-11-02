package com.jacopo.mygymtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val routineId: Int = 0,
    val name: String,
    val note: String? = null,
)
