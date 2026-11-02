package com.jacopo.mygymtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A workout performed from a routine. [finishedAtEpochMs] is `null` while the workout is in progress.
 * [routineName] is a snapshot, so history keeps its label after the routine is renamed or deleted.
 */
@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["routineId"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("routineId")],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Int = 0,
    val routineId: Int?,
    val routineName: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long? = null,
)
