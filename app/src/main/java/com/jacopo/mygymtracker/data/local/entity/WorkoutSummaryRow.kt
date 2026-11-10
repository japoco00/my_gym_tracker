package com.jacopo.mygymtracker.data.local.entity

/** Query result: a finished workout session with aggregates over its completed sets. */
data class WorkoutSummaryRow(
    val sessionId: Int,
    val routineId: Int?,
    val routineName: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val setCount: Int,
    val volumeKg: Double,
)