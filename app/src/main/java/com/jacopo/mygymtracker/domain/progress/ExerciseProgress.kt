package com.jacopo.mygymtracker.domain.progress

import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.ExerciseHistoryEntry
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.SetType
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.domain.model.WorkoutSession
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

/** Warm-ups are ramp-up work: they count toward no volume, record or progress figure. */
private val LoggedSet.isWarmUp: Boolean get() = type == SetType.WARM_UP

/** Total load moved: the sum of reps x weight, warm-ups excluded. */
fun List<LoggedSet>.volumeKg(): Double = sumOf { if (it.isWarmUp) 0.0 else it.reps * it.weightKg }

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
 * nothing earlier to beat: a first time isn't a record. Warm-ups neither set nor hold records.
 */
fun prSetIndex(sets: List<LoggedSet>, earlier: List<LoggedSet>, trackingType: TrackingType): Int? {
    val metric = trackingType.progressMetrics().first()
    val toBeat = earlier.filterNot { it.isWarmUp }.mapNotNull(metric::setValue).maxOrNull() ?: return null
    val best = sets.indices
        .filterNot { sets[it].isWarmUp }
        .mapNotNull { index -> metric.setValue(sets[index])?.let { index to it } }
        .maxByOrNull { (_, value) -> value }
        ?: return null
    return best.first.takeIf { best.second > toBeat }
}

/** A [set] of [exercise] that beat every earlier one, see [prSetIndex]. */
data class PersonalRecord(val exercise: Exercise, val set: LoggedSet, val achievedAt: Instant)

/** The newest records in [workouts], given oldest first: at most one per exercise and [limit] in all, newest first. */
fun latestRecords(workouts: List<WorkoutSession>, limit: Int): List<PersonalRecord> {
    val earlierSets = HashMap<Int?, MutableList<LoggedSet>>()
    val records = mutableListOf<PersonalRecord>()
    for (workout in workouts) {
        for (performed in workout.exercises) {
            val earlier = earlierSets.getOrPut(performed.exercise.id) { mutableListOf() }
            prSetIndex(performed.sets, earlier, performed.exercise.trackingType)?.let {
                records += PersonalRecord(performed.exercise, performed.sets[it], workout.startedAt)
            }
            earlier += performed.sets
        }
    }
    return records.asReversed().distinctBy { it.exercise.id }.take(limit)
}

/**
 * The set that ranks first on this type's main metric, then on weight, reps and duration, e.g. the
 * top e1RM set for weight × reps. `null` without working sets.
 */
fun TrackingType.bestSet(sets: List<LoggedSet>): LoggedSet? {
    val metric = progressMetrics().first()
    return sets.filterNot { it.isWarmUp }.maxWithOrNull(
        compareBy({ metric.setValue(it) ?: 0.0 }, { it.weightKg }, { it.reps }, { it.durationSeconds }),
    )
}

/**
 * How one exercise went in one workout.
 *
 * @property bestSet see [TrackingType.bestSet].
 */
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
    val bestSet: LoggedSet? = null,
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

/** `null` when only warm-ups were done, as they don't count toward progress. */
fun ExerciseHistoryEntry.toSessionProgress(trackingType: TrackingType): SessionProgress? {
    val working = sets.filterNot { it.isWarmUp }.ifEmpty { return null }
    return SessionProgress(
        sessionId = sessionId,
        performedAt = performedAt,
        heaviestKg = working.maxOf { it.weightKg },
        estimatedOneRepMaxKg = working.mapNotNull { estimatedOneRepMax(it.reps, it.weightKg) }.maxOrNull(),
        volumeKg = working.volumeKg(),
        mostReps = working.maxOf { it.reps },
        totalReps = working.sumOf { it.reps },
        longestSeconds = working.maxOf { it.durationSeconds },
        totalSeconds = working.sumOf { it.durationSeconds },
        bestSet = trackingType.bestSet(working),
    )
}

/**
 * The workout of [this] chronological list holding the record for [metric]: the best value, the first
 * time it was achieved. `null` if no workout has a positive value.
 */
fun List<SessionProgress>.recordFor(metric: ProgressMetric): SessionProgress? =
    maxByOrNull { it.valueOf(metric) ?: 0.0 }?.takeIf { (it.valueOf(metric) ?: 0.0) > 0 }