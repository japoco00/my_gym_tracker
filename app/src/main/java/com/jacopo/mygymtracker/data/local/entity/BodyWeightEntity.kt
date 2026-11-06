package com.jacopo.mygymtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One body weight entry per day, keyed by [dateEpochDay] (`LocalDate.toEpochDay()`). */
@Entity(tableName = "body_weights")
data class BodyWeightEntity(
    @PrimaryKey
    val dateEpochDay: Long,
    val weightKg: Double,
)
