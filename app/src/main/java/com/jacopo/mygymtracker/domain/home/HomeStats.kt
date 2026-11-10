package com.jacopo.mygymtracker.domain.home

import com.jacopo.mygymtracker.domain.history.groupByWeek
import com.jacopo.mygymtracker.domain.history.weekStart
import com.jacopo.mygymtracker.domain.history.weekStreak
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.WorkoutSummary
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** About a blue whale's weight: Home's yardstick for the total volume lifted. */
const val BLUE_WHALE_KG = 150_000.0

/** Weeks covered by Home's consistency heatmap, ending with the current one. */
const val HEATMAP_WEEKS = 18

/** Weeks covered by Home's weekly volume bars, ending with the current one. */
const val VOLUME_WEEKS = 8

data class WeekVolume(val start: LocalDate, val volumeKg: Double)

/**
 * Home's numbers, from the whole history.
 *
 * @property trainedDays the days with a workout since [heatmapStart], each with the id of its latest workout.
 * @property weeklyVolume the last [VOLUME_WEEKS] weeks, oldest first.
 * @property favouriteDay the weekday with the most workouts; ties go to the most recent.
 */
data class HomeStats(
    val weekStreak: Int,
    val monthWorkouts: Int,
    val monthVolumeKg: Double,
    val heatmapStart: LocalDate,
    val trainedDays: Map<LocalDate, Int>,
    val heatmapWorkouts: Int,
    val weeklyVolume: List<WeekVolume>,
    val favouriteDay: DayOfWeek,
    val totalVolumeKg: Double,
    val longestWorkout: WorkoutSummary,
    val totalWorkouts: Int,
    val firstWorkoutDay: LocalDate,
)

/** `null` without workouts. [workouts] are newest first, as in the history. */
fun homeStats(workouts: List<WorkoutSummary>, today: LocalDate, zone: ZoneId, firstDayOfWeek: DayOfWeek): HomeStats? {
    if (workouts.isEmpty()) return null
    fun WorkoutSummary.day(): LocalDate = startedAt.atZone(zone).toLocalDate()
    val thisWeek = today.weekStart(firstDayOfWeek)
    val heatmapStart = thisWeek.minusWeeks(HEATMAP_WEEKS - 1L)
    val inHeatmap = workouts.filter { it.day() >= heatmapStart }
    val inMonth = workouts.filter { YearMonth.from(it.day()) == YearMonth.from(today) }
    val weeks = groupByWeek(workouts, zone, firstDayOfWeek)
    val volumeByWeek = weeks.associate { week -> week.start to week.workouts.sumOf { it.volumeKg } }
    return HomeStats(
        weekStreak = weekStreak(weeks, today, firstDayOfWeek),
        monthWorkouts = inMonth.size,
        monthVolumeKg = inMonth.sumOf { it.volumeKg },
        heatmapStart = heatmapStart,
        // Oldest first, so each day ends up with its latest workout.
        trainedDays = inHeatmap.asReversed().associate { it.day() to it.id },
        heatmapWorkouts = inHeatmap.size,
        weeklyVolume = (VOLUME_WEEKS - 1 downTo 0).map { back ->
            val start = thisWeek.minusWeeks(back.toLong())
            WeekVolume(start, volumeByWeek[start] ?: 0.0)
        },
        favouriteDay = workouts.groupingBy { it.day().dayOfWeek }.eachCount().maxBy { it.value }.key,
        totalVolumeKg = workouts.sumOf { it.volumeKg },
        longestWorkout = workouts.maxBy { Duration.between(it.startedAt, it.finishedAt) },
        totalWorkouts = workouts.size,
        firstWorkoutDay = workouts.last().day(),
    )
}

/**
 * The routine to train next: the one after [lastRoutineId]'s in list order, wrapping around, or the first if
 * that one isn't found. Routines without exercises are skipped; `null` if none has any.
 */
fun upNext(routines: List<Routine>, lastRoutineId: Int?): Routine? {
    val last = lastRoutineId?.let { id -> routines.indexOfFirst { it.id == id } } ?: -1
    return (1..routines.size)
        .map { routines[(last + it).mod(routines.size)] }
        .firstOrNull { it.exercises.isNotEmpty() }
}
