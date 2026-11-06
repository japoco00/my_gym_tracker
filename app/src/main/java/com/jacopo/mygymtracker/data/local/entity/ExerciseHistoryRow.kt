package com.jacopo.mygymtracker.data.local.entity

import com.jacopo.mygymtracker.domain.model.SetType

/** Query result: one completed set of an exercise, with the session it belongs to. */
data class ExerciseHistoryRow(
    val sessionId: Int,
    val startedAtEpochMs: Long,
    val reps: Int,
    val weightKg: Double,
    val durationSeconds: Int,
    val type: SetType,
)