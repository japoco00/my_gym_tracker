package com.jacopo.mygymtracker.domain.home

import com.jacopo.mygymtracker.domain.model.WorkoutSession
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** From this many days without training, Home welcomes you back instead of pushing the next session. */
const val AWAY_AFTER_DAYS = 8

enum class TimeOfDay { MORNING, AFTERNOON, EVENING, NIGHT }

fun LocalTime.timeOfDay(): TimeOfDay = when (hour) {
    in 5..11 -> TimeOfDay.MORNING
    in 12..17 -> TimeOfDay.AFTERNOON
    in 18..21 -> TimeOfDay.EVENING
    else -> TimeOfDay.NIGHT
}

/** What the user is up to, which sets Home's tone. */
sealed interface HomeSituation {
    data class Active(val workout: WorkoutSession) : HomeSituation

    /** No finished workout yet. */
    data object New : HomeSituation

    data object TrainedToday : HomeSituation

    /** [days] since the last workout, at least [AWAY_AFTER_DAYS]. */
    data class Away(val days: Int) : HomeSituation

    data object Usual : HomeSituation
}

/** [lastWorkoutDay] is the day the newest finished workout started, `null` without history. */
fun homeSituation(active: WorkoutSession?, lastWorkoutDay: LocalDate?, today: LocalDate): HomeSituation {
    if (active != null) return HomeSituation.Active(active)
    if (lastWorkoutDay == null) return HomeSituation.New
    val days = ChronoUnit.DAYS.between(lastWorkoutDay, today).toInt()
    return when {
        days <= 0 -> HomeSituation.TrainedToday
        days >= AWAY_AFTER_DAYS -> HomeSituation.Away(days)
        else -> HomeSituation.Usual
    }
}

/** Home's title; the UI words each phrase and highlights its key words. A `null` name means none is set. */
sealed interface Greeting {
    /** "Good evening, Jacopo", or "Still up?" at night. */
    data class Hello(val timeOfDay: TimeOfDay, val name: String?) : Greeting
    data class Welcome(val name: String?) : Greeting
    data object FirstRoutine : Greeting
    data class StillRunning(val routineName: String) : Greeting
    data object SetsMissYou : Greeting
    data class SetsDone(val done: Int, val total: Int) : Greeting
    data object NiceWork : Greeting
    data object RestIsTraining : Greeting
    data object GoEat : Greeting
    data class DaysOff(val days: Int) : Greeting
    data object BarMissedYou : Greeting
    data class EasyRestart(val routineName: String) : Greeting
    data object NextSession : Greeting
    data class Calling(val routineName: String) : Greeting
    data class WeeksInARow(val weeks: Int) : Greeting
}

/**
 * One phrase from the pool that fits [situation]. [seed] picks which, so a screen keeps its phrase while the data
 * updates. [upNext] is the next routine's name, if any; [weekStreak] the weeks in a row with workouts.
 */
fun greeting(
    situation: HomeSituation,
    timeOfDay: TimeOfDay,
    name: String,
    upNext: String?,
    weekStreak: Int,
    seed: Int,
): Greeting {
    val named = name.ifBlank { null }
    val hello = Greeting.Hello(timeOfDay, named)
    val pool = when (situation) {
        is HomeSituation.Active -> {
            val sets = situation.workout.exercises.flatMap { it.sets }
            listOf(
                Greeting.StillRunning(situation.workout.routineName),
                Greeting.SetsMissYou,
                Greeting.SetsDone(sets.count { it.isCompleted }, sets.size),
            )
        }
        HomeSituation.New -> listOf(Greeting.Welcome(named), upNext?.let(Greeting::Calling) ?: Greeting.FirstRoutine)
        HomeSituation.TrainedToday -> listOf(hello, Greeting.NiceWork, Greeting.RestIsTraining, Greeting.GoEat)
        is HomeSituation.Away ->
            listOfNotNull(Greeting.DaysOff(situation.days), Greeting.BarMissedYou, upNext?.let(Greeting::EasyRestart))
        HomeSituation.Usual -> listOfNotNull(
            hello,
            Greeting.NextSession,
            upNext?.let(Greeting::Calling),
            Greeting.WeeksInARow(weekStreak).takeIf { weekStreak >= 2 },
        )
    }
    return pool[seed.mod(pool.size)]
}
