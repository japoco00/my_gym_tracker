package com.jacopo.mygymtracker.domain.model

/**
 * A workout routine: a named, ordered group of exercises with their planned sets.
 *
 * @property id database id, or `null` if the routine has not been persisted yet.
 */
data class Routine(
    val id: Int? = null,
    val name: String,
    val note: String? = null,
    val exercises: List<Exercise> = emptyList(),
)
