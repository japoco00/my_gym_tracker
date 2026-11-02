package com.jacopo.mygymtracker.domain.progress

import com.jacopo.mygymtracker.domain.model.ExerciseHistoryEntry
import com.jacopo.mygymtracker.domain.model.LoggedSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        val progress = entry(1, day = 1, LoggedSet(5, 100.0), LoggedSet(15, 60.0)).toSessionProgress()

        assertEquals(100.0, progress.heaviestKg, 1e-9)
        assertEquals(100.0 * (1 + 5 / 30.0), progress.estimatedOneRepMaxKg!!, 1e-9)
        assertEquals(500.0 + 900.0, progress.volumeKg, 1e-9)
    }

    @Test
    fun personalRecords_pickBestValueAndFirstDateAchieved() {
        val sessions = listOf(
            entry(1, day = 1, LoggedSet(5, 100.0)),
            entry(2, day = 2, LoggedSet(3, 110.0)),
            entry(3, day = 3, LoggedSet(10, 80.0), LoggedSet(3, 110.0)),
        ).map { it.toSessionProgress() }

        val records = sessions.personalRecords()

        assertEquals(PersonalRecord(110.0, day(2)), records[ProgressMetric.HEAVIEST_WEIGHT])
        assertEquals(PersonalRecord(110.0 * (1 + 3 / 30.0), day(2)), records[ProgressMetric.ESTIMATED_ONE_REP_MAX])
        assertEquals(PersonalRecord(1130.0, day(3)), records[ProgressMetric.SESSION_VOLUME])
    }

    @Test
    fun personalRecords_ignoreBodyweightOnlyHistory() {
        val records = listOf(entry(1, day = 1, LoggedSet(20, 0.0))).map { it.toSessionProgress() }.personalRecords()

        assertTrue(records.isEmpty())
    }

    private fun day(day: Long): Instant = Instant.EPOCH.plusSeconds(day * 86_400)

    private fun entry(sessionId: Int, day: Long, vararg sets: LoggedSet) =
        ExerciseHistoryEntry(sessionId = sessionId, performedAt = day(day), sets = sets.toList())
}