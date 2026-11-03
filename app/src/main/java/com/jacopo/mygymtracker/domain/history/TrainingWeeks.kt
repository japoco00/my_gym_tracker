package com.jacopo.mygymtracker.domain.history

import com.jacopo.mygymtracker.domain.model.WorkoutSummary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** The workouts started in the week beginning on [start], newest first. */
data class TrainingWeek(val start: LocalDate, val workouts: List<WorkoutSummary>)

/** The first day of this date's week, e.g. the Monday before or on it. */
fun LocalDate.weekStart(firstDayOfWeek: DayOfWeek): LocalDate =
    with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

/** Groups [workouts], newest first, by the week they started in; the newest week comes first. */
fun groupByWeek(workouts: List<WorkoutSummary>, zone: ZoneId, firstDayOfWeek: DayOfWeek): List<TrainingWeek> =
    workouts
        .groupBy { it.startedAt.atZone(zone).toLocalDate().weekStart(firstDayOfWeek) }
        .map { (start, inWeek) -> TrainingWeek(start, inWeek) }

/**
 * How many weeks in a row had at least one workout, counting back from [today]'s week.
 * A current week without workouts doesn't break the streak: it isn't over yet.
 */
fun weekStreak(weeks: List<TrainingWeek>, today: LocalDate, firstDayOfWeek: DayOfWeek): Int {
    val trained = weeks.mapTo(HashSet()) { it.start }
    var week = today.weekStart(firstDayOfWeek)
    if (week !in trained) week = week.minusWeeks(1)
    var streak = 0
    while (week in trained) {
        streak++
        week = week.minusWeeks(1)
    }
    return streak
}
