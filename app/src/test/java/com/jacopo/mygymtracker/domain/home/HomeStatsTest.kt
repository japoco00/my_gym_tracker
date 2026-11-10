package com.jacopo.mygymtracker.domain.home

import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.RoutineExercise
import com.jacopo.mygymtracker.domain.model.WorkoutSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class HomeStatsTest {

    // 2025-10-15 is a Wednesday; its week starts on Monday 2025-10-13.
    private val today = LocalDate.of(2025, 10, 15)

    @Test
    fun homeStats_isNullWithoutWorkouts() {
        assertNull(homeStats(emptyList(), today, ZoneOffset.UTC, DayOfWeek.MONDAY))
    }

    @Test
    fun homeStats_summarisesTheHistory() {
        val workouts = listOf(
            workout(1, "2025-10-14", minutes = 50, volumeKg = 3_000.0),
            workout(2, "2025-10-07", minutes = 90, volumeKg = 2_000.0),
            workout(3, "2025-09-30", minutes = 60, volumeKg = 1_000.0),
            workout(4, "2025-06-03", minutes = 40, volumeKg = 500.0),
        )

        val stats = homeStats(workouts, today, ZoneOffset.UTC, DayOfWeek.MONDAY)!!

        assertEquals(3, stats.weekStreak)
        assertEquals(2, stats.monthWorkouts)
        assertEquals(5_000.0, stats.monthVolumeKg, 1e-9)
        assertEquals(DayOfWeek.TUESDAY, stats.favouriteDay)
        assertEquals(6_500.0, stats.totalVolumeKg, 1e-9)
        assertEquals(2, stats.longestWorkout.id)
        assertEquals(4, stats.totalWorkouts)
        assertEquals(LocalDate.of(2025, 6, 3), stats.firstWorkoutDay)
    }

    @Test
    fun homeStats_heatmapCoversEighteenWeeksAndKeepsEachDaysLatestWorkout() {
        val workouts = listOf(
            workout(1, "2025-10-14", hour = 19),
            workout(2, "2025-10-14", hour = 8),
            workout(3, "2025-06-16"),
            workout(4, "2025-06-15"),
        )

        val stats = homeStats(workouts, today, ZoneOffset.UTC, DayOfWeek.MONDAY)!!

        assertEquals(LocalDate.of(2025, 6, 16), stats.heatmapStart)
        assertEquals(mapOf(LocalDate.of(2025, 10, 14) to 1, LocalDate.of(2025, 6, 16) to 3), stats.trainedDays)
        assertEquals(3, stats.heatmapWorkouts)
    }

    @Test
    fun homeStats_weeklyVolumeEndsWithTheCurrentWeek() {
        val workouts = listOf(
            workout(1, "2025-10-14", volumeKg = 1_000.0),
            workout(2, "2025-10-13", volumeKg = 500.0),
            workout(3, "2025-09-30", volumeKg = 2_000.0),
            workout(4, "2025-08-18", volumeKg = 4_000.0),
        )

        val weeks = homeStats(workouts, today, ZoneOffset.UTC, DayOfWeek.MONDAY)!!.weeklyVolume

        assertEquals(VOLUME_WEEKS, weeks.size)
        assertEquals(LocalDate.of(2025, 8, 25), weeks.first().start)
        assertEquals(LocalDate.of(2025, 10, 13), weeks.last().start)
        assertEquals(listOf(0.0, 0.0, 0.0, 0.0, 0.0, 2_000.0, 0.0, 1_500.0), weeks.map { it.volumeKg })
    }

    @Test
    fun upNext_followsTheLastRoutineAndSkipsEmptyOnes() {
        val routines = listOf(routine(1), routine(2, empty = true), routine(3))

        assertEquals(3, upNext(routines, lastRoutineId = 1)?.id)
        assertEquals(1, upNext(routines, lastRoutineId = 3)?.id)
        assertEquals(3, upNext(routines, lastRoutineId = 2)?.id)
        assertEquals(1, upNext(routines, lastRoutineId = null)?.id)
        assertEquals(1, upNext(routines, lastRoutineId = 99)?.id)
        assertEquals(1, upNext(listOf(routine(1)), lastRoutineId = 1)?.id)
        assertNull(upNext(listOf(routine(2, empty = true)), lastRoutineId = null))
        assertNull(upNext(emptyList(), lastRoutineId = null))
    }

    private fun workout(id: Int, date: String, hour: Int = 18, minutes: Long = 60, volumeKg: Double = 1_000.0): WorkoutSummary {
        val startedAt = LocalDate.parse(date).atTime(hour, 0).toInstant(ZoneOffset.UTC)
        return WorkoutSummary(
            id = id,
            routineName = "Push Day",
            startedAt = startedAt,
            finishedAt = startedAt.plusSeconds(minutes * 60),
            setCount = 10,
            volumeKg = volumeKg,
        )
    }

    private fun routine(id: Int, empty: Boolean = false) = Routine(
        id = id,
        name = "Routine $id",
        exercises = if (empty) emptyList() else listOf(RoutineExercise(Exercise(id = 1, name = "Squat", category = Category.LEGS))),
    )
}
