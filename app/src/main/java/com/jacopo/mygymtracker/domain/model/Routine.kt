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

/** A catalog [exercise] placed inside a routine, with its ordered planned [sets]. */
data class RoutineExercise(
    val exercise: Exercise,
    val sets: List<PlannedSet> = emptyList(),
)

/** A single planned set. Weight is always stored in kilograms. */
data class PlannedSet(
    val reps: Int,
    val weightKg: Double,
)