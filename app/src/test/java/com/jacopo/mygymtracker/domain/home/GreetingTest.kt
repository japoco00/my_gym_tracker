package com.jacopo.mygymtracker.domain.home

import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.WorkoutExercise
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class GreetingTest {

    private val today = LocalDate.of(2025, 10, 15)

    @Test
    fun timeOfDay_switchesAtFiveTwelveEighteenAndTwentyTwo() {
        val hours = mapOf(
            4 to TimeOfDay.NIGHT,
            5 to TimeOfDay.MORNING,
            11 to TimeOfDay.MORNING,
            12 to TimeOfDay.AFTERNOON,
            17 to TimeOfDay.AFTERNOON,
            18 to TimeOfDay.EVENING,
            21 to TimeOfDay.EVENING,
            22 to TimeOfDay.NIGHT,
        )

        hours.forEach { (hour, expected) -> assertEquals(expected, LocalTime.of(hour, 30).timeOfDay()) }
    }

    @Test
    fun homeSituation_dependsOnTheActiveWorkoutAndTheLastWorkoutDay() {
        val active = workout()

        assertEquals(HomeSituation.Active(active), homeSituation(active, today, today))
        assertEquals(HomeSituation.New, homeSituation(null, null, today))
        assertEquals(HomeSituation.TrainedToday, homeSituation(null, today, today))
        assertEquals(HomeSituation.Usual, homeSituation(null, today.minusDays(AWAY_AFTER_DAYS - 1L), today))
        assertEquals(HomeSituation.Away(AWAY_AFTER_DAYS), homeSituation(null, today.minusDays(AWAY_AFTER_DAYS.toLong()), today))
    }

    @Test
    fun greeting_seedPicksFromThePoolAndWrapsAround() {
        fun pick(seed: Int) = greeting(HomeSituation.Usual, TimeOfDay.EVENING, "Jacopo", "Pull Day", weekStreak = 3, seed)

        assertEquals(Greeting.Hello(TimeOfDay.EVENING, "Jacopo"), pick(0))
        assertEquals(Greeting.NextSession, pick(1))
        assertEquals(Greeting.Calling("Pull Day"), pick(2))
        assertEquals(Greeting.WeeksInARow(3), pick(3))
        assertEquals(pick(0), pick(4))
        assertEquals(pick(3), pick(-1))
    }

    @Test
    fun greeting_leavesOutPhrasesWithoutData() {
        fun pick(seed: Int) = greeting(HomeSituation.Usual, TimeOfDay.MORNING, " ", upNext = null, weekStreak = 1, seed)

        assertEquals(Greeting.Hello(TimeOfDay.MORNING, null), pick(0))
        assertEquals(Greeting.NextSession, pick(1))
        assertEquals(pick(0), pick(2))
        assertEquals(Greeting.FirstRoutine, greeting(HomeSituation.New, TimeOfDay.MORNING, "", null, 0, seed = 1))
        assertEquals(Greeting.Calling("Push Day"), greeting(HomeSituation.New, TimeOfDay.MORNING, "", "Push Day", 0, seed = 1))
    }

    @Test
    fun greeting_countsTheActiveWorkoutsSets() {
        val active = HomeSituation.Active(workout())

        assertEquals(Greeting.StillRunning("Push Day"), greeting(active, TimeOfDay.NIGHT, "", null, 0, seed = 0))
        assertEquals(Greeting.SetsDone(1, 3), greeting(active, TimeOfDay.NIGHT, "", null, 0, seed = 2))
    }

    @Test
    fun greeting_awayCountsTheDaysOff() {
        assertEquals(Greeting.DaysOff(12), greeting(HomeSituation.Away(12), TimeOfDay.EVENING, "", "Legs", 0, seed = 0))
        assertEquals(Greeting.EasyRestart("Legs"), greeting(HomeSituation.Away(12), TimeOfDay.EVENING, "", "Legs", 0, seed = 2))
    }

    private fun workout() = WorkoutSession(
        id = 1,
        routineId = 1,
        routineName = "Push Day",
        startedAt = Instant.EPOCH,
        exercises = listOf(
            WorkoutExercise(
                Exercise(id = 1, name = "Bench Press", category = Category.CHEST),
                listOf(LoggedSet(5, 80.0, isCompleted = true), LoggedSet(5, 80.0), LoggedSet(5, 80.0)),
            ),
        ),
    )
}
