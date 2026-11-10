package com.jacopo.mygymtracker.domain.progress

import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.ExerciseHistoryEntry
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.SetType
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.domain.model.WorkoutExercise
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class ExerciseProgressTest {

    @Test
    fun estimatedOneRepMax_usesEpleyWithinRange() {
        assertEquals(100.0, estimatedOneRepMax(reps = 1, weightKg = 100.0)!!, 1e-9)
        assertEquals(120.0, estimatedOneRepMax(reps = 6, weightKg = 100.0)!!, 1e-9)
        assertEquals(140.0, estimatedOneRepMax(reps = 12, weightKg = 100.0)!!, 1e-9)
    }

    @Test
    fun estimatedOneRepMax_isNullOutsideRangeOrWithoutLoad() {
        assertNull(estimatedOneRepMax(reps = 0, weightKg = 100.0))
        assertNull(estimatedOneRepMax(reps = 13, weightKg = 100.0))
        assertNull(estimatedOneRepMax(reps = 10, weightKg = 0.0))
    }

    @Test
    fun toSessionProgress_aggregatesSets() {
        val progress = entry(1, day = 1, LoggedSet(5, 100.0), LoggedSet(15, 60.0))
            .toSessionProgress(TrackingType.WEIGHT_REPS)!!

        assertEquals(100.0, progress.heaviestKg, 1e-9)
        assertEquals(100.0 * (1 + 5 / 30.0), progress.estimatedOneRepMaxKg!!, 1e-9)
        assertEquals(500.0 + 900.0, progress.volumeKg, 1e-9)
        assertEquals(15, progress.mostReps)
        assertEquals(20, progress.totalReps)
    }

    @Test
    fun toSessionProgress_aggregatesDurations() {
        val progress = entry(1, day = 1, LoggedSet(durationSeconds = 60), LoggedSet(durationSeconds = 45))
            .toSessionProgress(TrackingType.DURATION)!!

        assertEquals(60, progress.longestSeconds)
        assertEquals(105, progress.totalSeconds)
    }

    @Test
    fun progressMetrics_dependOnTrackingType() {
        assertEquals(ProgressMetric.ESTIMATED_ONE_REP_MAX, TrackingType.WEIGHT_REPS.progressMetrics().first())
        assertEquals(ProgressMetric.MOST_REPS, TrackingType.REPS.progressMetrics().first())
        assertEquals(
            listOf(ProgressMetric.LONGEST_SET, ProgressMetric.TOTAL_DURATION),
            TrackingType.DURATION.progressMetrics(),
        )
    }

    @Test
    fun toSessionProgress_picksTheBestSetByTheMainMetricThenWeight() {
        val sets = arrayOf(LoggedSet(5, 100.0), LoggedSet(8, 95.0), LoggedSet(15, 120.0))

        assertEquals(LoggedSet(8, 95.0), entry(1, day = 1, *sets).toSessionProgress(TrackingType.WEIGHT_REPS)!!.bestSet)
        assertEquals(LoggedSet(15, 120.0), entry(1, day = 1, *sets).toSessionProgress(TrackingType.REPS)!!.bestSet)
        assertEquals(
            LoggedSet(10, 5.0),
            entry(1, day = 1, LoggedSet(10), LoggedSet(10, 5.0)).toSessionProgress(TrackingType.REPS)!!.bestSet,
        )
    }

    @Test
    fun recordFor_isTheFirstWorkoutWithTheBestValue() {
        val sessions = listOf(
            entry(1, day = 1, LoggedSet(5, 100.0)),
            entry(2, day = 2, LoggedSet(3, 110.0)),
            entry(3, day = 3, LoggedSet(10, 80.0), LoggedSet(3, 110.0)),
        ).mapNotNull { it.toSessionProgress(TrackingType.WEIGHT_REPS) }

        assertEquals(2, sessions.recordFor(ProgressMetric.HEAVIEST_WEIGHT)?.sessionId)
        assertEquals(2, sessions.recordFor(ProgressMetric.ESTIMATED_ONE_REP_MAX)?.sessionId)
        assertEquals(3, sessions.recordFor(ProgressMetric.SESSION_VOLUME)?.sessionId)
    }

    @Test
    fun recordFor_isNullWithoutAPositiveValue() {
        val sessions = listOf(entry(1, day = 1, LoggedSet(20, 0.0))).mapNotNull { it.toSessionProgress(TrackingType.REPS) }

        assertNull(sessions.recordFor(ProgressMetric.HEAVIEST_WEIGHT))
        assertEquals(1, sessions.recordFor(ProgressMetric.MOST_REPS)?.sessionId)
    }

    @Test
    fun prSetIndex_isTheBestSetWhenItBeatsEveryEarlierSet() {
        val earlier = listOf(LoggedSet(5, 100.0), LoggedSet(8, 90.0))
        val sets = listOf(LoggedSet(5, 100.0), LoggedSet(5, 105.0), LoggedSet(4, 105.0))

        assertEquals(1, prSetIndex(sets, earlier, TrackingType.WEIGHT_REPS))
    }

    @Test
    fun prSetIndex_isNullForATieOrAFirstTime() {
        val sets = listOf(LoggedSet(5, 100.0))

        assertNull(prSetIndex(sets, earlier = listOf(LoggedSet(5, 100.0)), TrackingType.WEIGHT_REPS))
        assertNull(prSetIndex(sets, earlier = emptyList(), TrackingType.WEIGHT_REPS))
    }

    @Test
    fun prSetIndex_usesTheTrackingTypesMainMetric() {
        val earlier = listOf(LoggedSet(reps = 12, durationSeconds = 60))

        assertEquals(0, prSetIndex(listOf(LoggedSet(reps = 13)), earlier, TrackingType.REPS))
        assertEquals(0, prSetIndex(listOf(LoggedSet(durationSeconds = 61)), earlier, TrackingType.DURATION))
        assertNull(prSetIndex(listOf(LoggedSet(reps = 12)), earlier, TrackingType.REPS))
    }

    @Test
    fun warmUps_countTowardNoVolumeOrRecord() {
        val warmUp = LoggedSet(5, 200.0, type = SetType.WARM_UP)
        val sets = listOf(warmUp, LoggedSet(5, 100.0))

        assertEquals(500.0, sets.volumeKg(), 1e-9)
        assertEquals(LoggedSet(5, 100.0), TrackingType.WEIGHT_REPS.bestSet(sets))
        assertNull(prSetIndex(sets, earlier = listOf(LoggedSet(5, 150.0)), TrackingType.WEIGHT_REPS))
        assertEquals(1, prSetIndex(sets, earlier = listOf(warmUp, LoggedSet(5, 90.0)), TrackingType.WEIGHT_REPS))
    }

    @Test
    fun toSessionProgress_ignoresWarmUpsAndIsNullWithOnlyWarmUps() {
        val warmUp = LoggedSet(5, 200.0, type = SetType.WARM_UP)

        val progress = entry(1, day = 1, warmUp, LoggedSet(5, 100.0)).toSessionProgress(TrackingType.WEIGHT_REPS)

        assertEquals(100.0, progress!!.heaviestKg, 1e-9)
        assertNull(entry(1, day = 1, warmUp).toSessionProgress(TrackingType.WEIGHT_REPS))
    }

    @Test
    fun latestRecords_keepsTheNewestRecordPerExercise() {
        val bench = Exercise(id = 1, name = "Bench Press", category = Category.CHEST)
        val squat = Exercise(id = 2, name = "Squat", category = Category.LEGS)
        val dips = Exercise(id = 3, name = "Dips", category = Category.CHEST, trackingType = TrackingType.REPS)
        val workouts = listOf(
            workout(day = 1, bench to LoggedSet(5, 80.0), squat to LoggedSet(5, 100.0), dips to LoggedSet(10)),
            workout(day = 2, bench to LoggedSet(5, 85.0), squat to LoggedSet(5, 100.0)),
            workout(day = 3, bench to LoggedSet(5, 90.0), dips to LoggedSet(12)),
            workout(day = 4, squat to LoggedSet(5, 90.0)),
        )

        val records = latestRecords(workouts, limit = 3)

        assertEquals(listOf("Dips" to day(3), "Bench Press" to day(3)), records.map { it.exercise.name to it.achievedAt })
        assertEquals(LoggedSet(5, 90.0), records[1].set)
        assertEquals(listOf("Dips"), latestRecords(workouts, limit = 1).map { it.exercise.name })
    }

    private fun day(day: Long): Instant = Instant.EPOCH.plusSeconds(day * 86_400)

    private fun workout(day: Long, vararg sets: Pair<Exercise, LoggedSet>) = WorkoutSession(
        routineId = null,
        routineName = "Workout",
        startedAt = day(day),
        exercises = sets.map { (exercise, set) -> WorkoutExercise(exercise, listOf(set)) },
    )

    private fun entry(sessionId: Int, day: Long, vararg sets: LoggedSet) =
        ExerciseHistoryEntry(sessionId = sessionId, performedAt = day(day), sets = sets.toList())
}