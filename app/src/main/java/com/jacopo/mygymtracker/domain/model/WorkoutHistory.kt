package com.jacopo.mygymtracker.domain.model

import java.time.Instant

/**
 * A finished workout as listed in the history. [volumeKg] is the sum of reps x weight, warm-ups excluded.
 *
 * @property routineId the routine it was started from; `null` for empty workouts or once that routine is deleted.
 */
data class WorkoutSummary(
    val id: Int,
    val routineName: String,
    val startedAt: Instant,
    val finishedAt: Instant,
    val setCount: Int,
    val volumeKg: Double,
    val routineId: Int? = null,
)

/** The completed sets of one exercise in one finished workout. */
data class ExerciseHistoryEntry(
    val sessionId: Int,
    val performedAt: Instant,
    val sets: List<LoggedSet>,
)