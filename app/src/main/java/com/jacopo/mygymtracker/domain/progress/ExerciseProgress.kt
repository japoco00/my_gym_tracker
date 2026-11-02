package com.jacopo.mygymtracker.domain.progress

import com.jacopo.mygymtracker.domain.model.ExerciseHistoryEntry
import com.jacopo.mygymtracker.domain.model.LoggedSet
import java.time.Instant

/** Above this many reps, one-rep-max estimates become unreliable. */
const val MAX_ESTIMATE_REPS = 12

/**
 * Epley estimate of the one-rep max, `weight x (1 + reps / 30)`.
 * A single rep is the weight itself. `null` without load or beyond [MAX_ESTIMATE_REPS].
 */
fun estimatedOneRepMax(reps: Int, weightKg: Double): Double? = when {
    weightKg <= 0 || reps !in 1..MAX_ESTIMATE_REPS -> null
    reps == 1 -> weightKg
    else -> weightKg * (1 + reps / 30.0)
}

/** Total load moved: the sum of reps x weight. */
fun List<LoggedSet>.volumeKg(): Double = sumOf { it.reps * it.weightKg }

enum class ProgressMetric { ESTIMATED_ONE_REP_MAX, HEAVIEST_WEIGHT, SESSION_VOLUME }

/** How one exercise went in one workout. */
data class SessionProgress(
    val sessionId: Int,
    val performedAt: Instant,
    val heaviestKg: Double,
    val estimatedOneRepMaxKg: Double?,
    val volumeKg: Double,
) {
    fun valueOf(metric: ProgressMetric): Double? = when (metric) {
        ProgressMetric.ESTIMATED_ONE_REP_MAX -> estimatedOneRepMaxKg
        ProgressMetric.HEAVIEST_WEIGHT -> heaviestKg
        ProgressMetric.SESSION_VOLUME -> volumeKg
    }
}

fun ExerciseHistoryEntry.toSessionProgress(): SessionProgress = SessionProgress(
    sessionId = sessionId,
    performedAt = performedAt,
    heaviestKg = sets.maxOfOrNull { it.weightKg } ?: 0.0,
    estimatedOneRepMaxKg = sets.mapNotNull { estimatedOneRepMax(it.reps, it.weightKg) }.maxOrNull(),
    volumeKg = sets.volumeKg(),
)

data class PersonalRecord(
    val valueKg: Double,
    val achievedAt: Instant,
)

/**
 * Best value per metric across [this] chronological list. Ties keep the first time it was achieved.
 * Metrics without any positive value are absent.
 */
fun List<SessionProgress>.personalRecords(): Map<ProgressMetric, PersonalRecord> =
    ProgressMetric.entries
        .mapNotNull { metric ->
            this.mapNotNull { session ->
                session.valueOf(metric)
                    ?.takeIf { it > 0 }
                    ?.let { PersonalRecord(valueKg = it, achievedAt = session.performedAt) }
            }
                .maxByOrNull { it.valueKg }
                ?.let { metric to it }
        }
        .toMap()