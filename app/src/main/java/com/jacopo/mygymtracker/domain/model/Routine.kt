package com.jacopo.mygymtracker.domain.model

/**
 * A workout routine: a named, ordered list of exercises with their planned sets.
 *
 * @property id database id, or `null` if the routine has not been persisted yet.
 */
data class Routine(
    val id: Int? = null,
    val name: String,
    val note: String? = null,
    val exercises: List<RoutineExercise> = emptyList(),
)

/**
 * A catalog [exercise] placed inside a routine, with its ordered planned [sets].
 * [restSeconds] is the rest after each set; `null` means the default from the settings.
 */
data class RoutineExercise(
    val exercise: Exercise,
    val sets: List<PlannedSet> = emptyList(),
    val restSeconds: Int? = null,
)

/**
 * A single planned set. Weight is always stored in kilograms.
 * Which fields matter depends on the exercise's [TrackingType]; the others stay 0.
 */
data class PlannedSet(
    val reps: Int = 0,
    val weightKg: Double = 0.0,
    val durationSeconds: Int = 0,
)