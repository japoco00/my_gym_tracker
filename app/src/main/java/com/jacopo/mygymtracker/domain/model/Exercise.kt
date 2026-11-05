package com.jacopo.mygymtracker.domain.model

/**
 * A reusable exercise from the global catalog.
 *
 * @property id catalog id, or `null` if the exercise has not been persisted yet.
 * @property isBarbell loaded with plates, so the set editor shows the plates per side.
 */
data class Exercise(
    val id: Int? = null,
    val name: String,
    val category: Category,
    val trackingType: TrackingType = TrackingType.WEIGHT_REPS,
    val note: String? = null,
    val isBarbell: Boolean = false,
)