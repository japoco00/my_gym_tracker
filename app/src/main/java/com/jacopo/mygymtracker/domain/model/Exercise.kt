package com.jacopo.mygymtracker.domain.model

/**
 * An exercise from the reusable catalog, optionally carrying the sets planned for it
 * inside a specific routine.
 *
 * @property id catalog id, or `null` if the exercise has not been persisted yet.
 */
data class Exercise(
    val id: Int? = null,
    val name: String,
    val category: Category,
    val note: String? = null,
    val sets: List<ExerciseSet> = emptyList(),
)

/** A single planned set of an [Exercise]. */
data class ExerciseSet(
    val id: Int? = null,
    val reps: Int,
    val weight: Int,
)
