package com.jacopo.mygymtracker.domain.model

/**
 * A reusable exercise from the global catalog.
 *
 * @property id catalog id, or `null` if the exercise has not been persisted yet.
 */
data class Exercise(
    val id: Int? = null,
    val name: String,
    val category: Category,
    val note: String? = null,
)