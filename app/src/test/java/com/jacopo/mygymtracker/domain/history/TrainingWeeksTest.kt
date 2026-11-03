package com.jacopo.mygymtracker.domain.history

import com.jacopo.mygymtracker.domain.model.WorkoutSummary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class TrainingWeeksTest {

    // 2025-10-15 is a Wednesday.
    private val today = LocalDate.of(2025, 10, 15)

    @Test
    fun weekStart_dependsOnTheFirstDayOfWeek() {
        assertEquals(LocalDate.of(2025, 10, 13), today.weekStart(DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2025, 10, 12), today.weekStart(DayOfWeek.SUNDAY))
        assertEquals(LocalDate.of(2025, 10, 13), LocalDate.of(2025, 10, 13).weekStart(DayOfWeek.MONDAY))
    }

    @Test
    fun groupByWeek_keepsNewestFirst() {
        val weeks = groupByWeek(
            listOf(workout(1, "2025-10-14"), workout(2, "2025-10-13"), workout(3, "2025-10-12"), workout(4, "2025-09-30")),
            ZoneOffset.UTC,
            DayOfWeek.MONDAY,
        )

        assertEquals(
            listOf(LocalDate.of(2025, 10, 13), LocalDate.of(2025, 10, 6), LocalDate.of(2025, 9, 29)),
            weeks.map { it.start },
        )
        assertEquals(listOf(1, 2), weeks[0].workouts.map { it.id })
    }

    @Test
    fun weekStreak_countsConsecutiveWeeks() {
        val weeks = weeksOf("2025-10-14", "2025-10-07", "2025-09-30", "2025-09-16")

        assertEquals(3, weekStreak(weeks, today, DayOfWeek.MONDAY))
    }

    @Test
    fun weekStreak_isNotBrokenByAnEmptyCurrentWeek() {
        val weeks = weeksOf("2025-10-07", "2025-09-30")

        assertEquals(2, weekStreak(weeks, today, DayOfWeek.MONDAY))
    }

    @Test
    fun weekStreak_isZeroAfterAMissedWeek() {
        assertEquals(0, weekStreak(weeksOf("2025-09-30"), today, DayOfWeek.MONDAY))
        assertEquals(0, weekStreak(emptyList(), today, DayOfWeek.MONDAY))
    }

    private fun weeksOf(vararg dates: String): List<TrainingWeek> =
        groupByWeek(dates.mapIndexed { index, date -> workout(index, date) }, ZoneOffset.UTC, DayOfWeek.MONDAY)

    private fun workout(id: Int, date: String): WorkoutSummary {
        val startedAt = LocalDate.parse(date).atTime(18, 0).toInstant(ZoneOffset.UTC)
        return WorkoutSummary(
            id = id,
            routineName = "Push Day",
            startedAt = startedAt,
            finishedAt = startedAt.plusSeconds(3_600),
            setCount = 10,
            volumeKg = 1_000.0,
        )
    }
}
