package com.jacopo.mygymtracker.domain.model

/** User preferences that affect how workouts are logged. */
data class UserSettings(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val restSeconds: Int = DEFAULT_REST_SECONDS,
) {
    companion object {
        const val DEFAULT_REST_SECONDS = 90
        const val REST_STEP_SECONDS = 15
    }
}