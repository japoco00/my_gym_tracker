package com.jacopo.mygymtracker.domain.model

/**
 * User preferences that affect how workouts are logged.
 *
 * @property plates the plates owned, per unit, heaviest first; each unit keeps its own set.
 */
data class UserSettings(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val restSeconds: Int = DEFAULT_REST_SECONDS,
    val plates: Map<WeightUnit, List<Double>> = WeightUnit.entries.associateWith { it.defaultPlates },
) {
    /** The plates owned in the current [weightUnit]. */
    val currentPlates: List<Double> get() = plates[weightUnit].orEmpty()

    companion object {
        const val DEFAULT_REST_SECONDS = 90
        const val REST_STEP_SECONDS = 15
    }
}