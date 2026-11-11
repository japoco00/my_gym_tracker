package com.jacopo.mygymtracker.widget

import com.jacopo.mygymtracker.domain.home.HomeSituation
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.RoutineExercise
import com.jacopo.mygymtracker.domain.model.WorkoutExercise
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import com.jacopo.mygymtracker.domain.model.WorkoutSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class WidgetDataTest {

    // 2025-10-15 is a Wednesday; its week starts on Monday 2025-10-13.
    private val today = LocalDate.of(2025, 10, 15)

    @Test
    fun activeWorkout_isResumedWithItsProgress() {
        val squat = Exercise(id = 1, name = "Squat", category = Category.LEGS)
        val active = WorkoutSession(
            id = 7,
            routineId = 1,
            routineName = "Push Day",
            startedAt = today.atTime(18, 0).toInstant(ZoneOffset.UTC),
            exercises = listOf(WorkoutExercise(squat, listOf(LoggedSet(isCompleted = true), LoggedSet()))),
        )

        val data = widgetData(listOf(routine(1)), active, emptyList(), today, ZoneOffset.UTC, DayOfWeek.MONDAY)

        assertEquals(NextUp.Resume(sessionId = 7, routineName = "Push Day", completedSets = 1, totalSets = 2), data.next)
    }

    @Test
    fun history_givesTheNextRoutineThisWeeksDaysAndTheStreak() {
        val history = listOf(
            workout(3, "2025-10-15", routineId = 1),
            workout(2, "2025-10-13", routineId = 2),
            workout(1, "2025-10-07", routineId = 1),
        )

        val data = widgetData(listOf(routine(1), routine(2)), null, history, today, ZoneOffset.UTC, DayOfWeek.MONDAY)

        assertEquals(NextUp.Start(2, "Routine 2", exerciseCount = 1, situation = HomeSituation.TrainedToday), data.next)
        assertEquals(LocalDate.of(2025, 10, 13), data.weekStart)
        assertEquals(setOf(LocalDate.of(2025, 10, 13), today), data.trainedDays)
        assertEquals(2, data.weekStreak)
    }

    @Test
    fun withoutTrainableRoutines_asksToCreateOne() {
        val data = widgetData(listOf(routine(1, empty = true)), null, emptyList(), today, ZoneOffset.UTC, DayOfWeek.MONDAY)

        assertEquals(NextUp.CreateRoutine, data.next)
        assertFalse(data.hasHistory)
        assertEquals(emptySet<LocalDate>(), data.trainedDays)
    }

    private fun workout(id: Int, date: String, routineId: Int): WorkoutSummary {
        val startedAt = LocalDate.parse(date).atTime(18, 0).toInstant(ZoneOffset.UTC)
        return WorkoutSummary(
            id = id,
            routineName = "Routine $routineId",
            startedAt = startedAt,
            finishedAt = startedAt.plusSeconds(3_600),
            setCount = 10,
            volumeKg = 1_000.0,
            routineId = routineId,
        )
    }

    private fun routine(id: Int, empty: Boolean = false) = Routine(
        id = id,
        name = "Routine $id",
        exercises = if (empty) emptyList() else listOf(RoutineExercise(Exercise(id = 1, name = "Squat", category = Category.LEGS))),
    )
}
