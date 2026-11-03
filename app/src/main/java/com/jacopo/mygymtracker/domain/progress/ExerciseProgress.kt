package com.jacopo.mygymtracker.domain.progress

import com.jacopo.mygymtracker.domain.model.ExerciseHistoryEntry
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.TrackingType
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

/** What a [ProgressMetric] measures, which decides how it is formatted and charted. */
enum class MetricQuantity { WEIGHT_KG, VOLUME_KG, REPS, SECONDS }

enum class ProgressMetric(val quantity: MetricQuantity) {
    ESTIMATED_ONE_REP_MAX(MetricQuantity.WEIGHT_KG),
    HEAVIEST_WEIGHT(MetricQuantity.WEIGHT_KG),
    SESSION_VOLUME(MetricQuantity.VOLUME_KG),
    MOST_REPS(MetricQuantity.REPS),
    TOTAL_REPS(MetricQuantity.REPS),
    LONGEST_SET(MetricQuantity.SECONDS),
    TOTAL_DURATION(MetricQuantity.SECONDS),
}

/** The metrics worth tracking for this type of exercise, in display order. The first is the default. */
fun TrackingType.progressMetrics(): List<ProgressMetric> = when (this) {
    TrackingType.WEIGHT_REPS -> listOf(
        ProgressMetric.ESTIMATED_ONE_REP_MAX,
        ProgressMetric.HEAVIEST_WEIGHT,
        ProgressMetric.SESSION_VOLUME,
    )
    TrackingType.REPS -> listOf(ProgressMetric.MOST_REPS, ProgressMetric.TOTAL_REPS, ProgressMetric.HEAVIEST_WEIGHT)
    TrackingType.DURATION -> listOf(ProgressMetric.LONGEST_SET, ProgressMetric.TOTAL_DURATION)
}

/** One set's value for a per-set metric (e1RM, heaviest, most reps, longest), `null` for per-workout totals. */
fun ProgressMetric.setValue(set: LoggedSet): Double? = when (this) {
    ProgressMetric.ESTIMATED_ONE_REP_MAX -> estimatedOneRepMax(set.reps, set.weightKg)
    ProgressMetric.HEAVIEST_WEIGHT -> set.weightKg
    ProgressMetric.MOST_REPS -> set.reps.toDouble()
    ProgressMetric.LONGEST_SET -> set.durationSeconds.toDouble()
    ProgressMetric.SESSION_VOLUME, ProgressMetric.TOTAL_REPS, ProgressMetric.TOTAL_DURATION -> null
}

/**
 * The set of [sets] that beat every [earlier] set on [trackingType]'s main metric, so a record
 * stays one even after it's beaten later. `null` if none did, a tie included, or if there's
 * nothing earlier to beat: a first time isn't a record.
 */
fun prSetIndex(sets: List<LoggedSet>, earlier: List<LoggedSet>, trackingType: TrackingType): Int? {
    val metric = trackingType.progressMetrics().first()
    val toBeat = earlier.mapNotNull(metric::setValue).maxOrNull() ?: return null
    val best = sets.indices
        .mapNotNull { index -> metric.setValue(sets[index])?.let { index to it } }
        .maxByOrNull { (_, value) -> value }
        ?: return null
    return best.first.takeIf { best.second > toBeat }
}

/** How one exercise went in one workout. */
data class SessionProgress(
    val sessionId: Int,
    val performedAt: Instant,
    val heaviestKg: Double,
    val estimatedOneRepMaxKg: Double?,
    val volumeKg: Double,
    val mostReps: Int,
    val totalReps: Int,
    val longestSeconds: Int,
    val totalSeconds: Int,
) {
    fun valueOf(metric: ProgressMetric): Double? = when (metric) {
        ProgressMetric.ESTIMATED_ONE_REP_MAX -> estimatedOneRepMaxKg
        ProgressMetric.HEAVIEST_WEIGHT -> heaviestKg
        ProgressMetric.SESSION_VOLUME -> volumeKg
        ProgressMetric.MOST_REPS -> mostReps.toDouble()
        ProgressMetric.TOTAL_REPS -> totalReps.toDouble()
        ProgressMetric.LONGEST_SET -> longestSeconds.toDouble()
        ProgressMetric.TOTAL_DURATION -> totalSeconds.toDouble()
    }
}

fun ExerciseHistoryEntry.toSessionProgress(): SessionProgress = SessionProgress(
    sessionId = sessionId,
    performedAt = performedAt,
    heaviestKg = sets.maxOfOrNull { it.weightKg } ?: 0.0,
    estimatedOneRepMaxKg = sets.mapNotNull { estimatedOneRepMax(it.reps, it.weightKg) }.maxOrNull(),
    volumeKg = sets.volumeKg(),
    mostReps = sets.maxOfOrNull { it.reps } ?: 0,
    totalReps = sets.sumOf { it.reps },
    longestSeconds = sets.maxOfOrNull { it.durationSeconds } ?: 0,
    totalSeconds = sets.sumOf { it.durationSeconds },
)

/** @property value in the unit of the metric's [MetricQuantity]. */
data class PersonalRecord(
    val value: Double,
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
                    ?.let { PersonalRecord(value = it, achievedAt = session.performedAt) }
            }
                .maxByOrNull { it.value }
                ?.let { metric to it }
        }
        .toMap()