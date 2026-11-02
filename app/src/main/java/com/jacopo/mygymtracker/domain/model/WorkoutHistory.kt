package com.jacopo.mygymtracker.domain.model

import java.time.Instant

/** A finished workout as listed in the history. [volumeKg] is the sum of reps x weight. */
data class WorkoutSummary(
    val id: Int,
    val routineName: String,
    val startedAt: Instant,
    val finishedAt: Instant,
    val setCount: Int,
    val volumeKg: Double,
)

/** The completed sets of one exercise in one finished workout. */
data class ExerciseHistoryEntry(
    val sessionId: Int,
    val performedAt: Instant,
    val sets: List<LoggedSet>,
)