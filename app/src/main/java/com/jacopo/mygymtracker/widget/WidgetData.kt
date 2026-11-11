package com.jacopo.mygymtracker.widget

import android.content.Context
import com.jacopo.mygymtracker.MyApplication
import com.jacopo.mygymtracker.domain.history.groupByWeek
import com.jacopo.mygymtracker.domain.history.weekStart
import com.jacopo.mygymtracker.domain.history.weekStreak
import com.jacopo.mygymtracker.domain.home.HomeSituation
import com.jacopo.mygymtracker.domain.home.homeSituation
import com.jacopo.mygymtracker.domain.home.upNext
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import com.jacopo.mygymtracker.domain.model.WorkoutSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * What the widgets show. Kept small on purpose: typing a weight in a workout doesn't change it,
 * so it doesn't redraw them.
 *
 * @property weekStart the first day of [today]'s week.
 * @property trainedDays the days of this week with a finished workout.
 * @property weekStreak weeks in a row with workouts.
 * @property hasHistory whether any workout was ever finished.
 */
data class WidgetData(
    val next: NextUp,
    val today: LocalDate,
    val weekStart: LocalDate,
    val trainedDays: Set<LocalDate>,
    val weekStreak: Int,
    val hasHistory: Boolean,
)

/** The widgets' answer to "what now?", behind their button. */
sealed interface NextUp {
    data class Resume(val sessionId: Int, val routineName: String, val completedSets: Int, val totalSets: Int) : NextUp

    /** [situation] sets the tone, and is never [HomeSituation.Active]. */
    data class Start(
        val routineId: Int,
        val routineName: String,
        val exerciseCount: Int,
        val situation: HomeSituation,
    ) : NextUp

    data object CreateRoutine : NextUp
}

/** [history] holds the finished workouts, newest first. */
fun widgetData(
    routines: List<Routine>,
    active: WorkoutSession?,
    history: List<WorkoutSummary>,
    today: LocalDate,
    zone: ZoneId,
    firstDayOfWeek: DayOfWeek,
): WidgetData {
    val weekStart = today.weekStart(firstDayOfWeek)
    val workoutDays = history.map { it.startedAt.atZone(zone).toLocalDate() }
    return WidgetData(
        next = nextUp(routines, active, history, workoutDays.firstOrNull(), today),
        today = today,
        weekStart = weekStart,
        trainedDays = workoutDays.filterTo(HashSet()) { it >= weekStart },
        weekStreak = weekStreak(groupByWeek(history, zone, firstDayOfWeek), today, firstDayOfWeek),
        hasHistory = history.isNotEmpty(),
    )
}

private fun nextUp(
    routines: List<Routine>,
    active: WorkoutSession?,
    history: List<WorkoutSummary>,
    lastWorkoutDay: LocalDate?,
    today: LocalDate,
): NextUp {
    if (active?.id != null) {
        val sets = active.exercises.flatMap { it.sets }
        return NextUp.Resume(active.id, active.routineName, sets.count { it.isCompleted }, sets.size)
    }
    val routine = upNext(routines, history.firstNotNullOfOrNull { it.routineId }) ?: return NextUp.CreateRoutine
    return NextUp.Start(
        routineId = checkNotNull(routine.id) { "Routines from the database have an id" },
        routineName = routine.name,
        exerciseCount = routine.exercises.size,
        situation = homeSituation(active = null, lastWorkoutDay = lastWorkoutDay, today = today),
    )
}

/** The widgets' data, live from the database; "today" is re-read on every change. */
fun Context.observeWidgetData(): Flow<WidgetData> {
    val container = (applicationContext as MyApplication).container
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    return combine(
        container.routineRepository.observeRoutines(),
        container.workoutRepository.observeActiveWorkout(),
        container.workoutRepository.observeHistory(),
    ) { routines, active, history ->
        val zone = ZoneId.systemDefault()
        widgetData(routines, active, history, LocalDate.now(zone), zone, firstDayOfWeek)
    }.distinctUntilChanged()
}
