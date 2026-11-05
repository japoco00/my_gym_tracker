package com.jacopo.mygymtracker.domain.model

/**
 * User preferences that affect how workouts are logged and how the app looks.
 *
 * @property plates the plates owned, per unit, heaviest first; each unit keeps its own set.
 * @property dynamicColor colours from the wallpaper, on Android 12+.
 * @property autoStartRest ticking a set starts the rest; otherwise it is started by hand.
 * @property keepScreenOn the screen stays on during a workout.
 */
data class UserSettings(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val restSeconds: Int = DEFAULT_REST_SECONDS,
    val plates: Map<WeightUnit, List<Double>> = WeightUnit.entries.associateWith { it.defaultPlates },
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val autoStartRest: Boolean = true,
    val keepScreenOn: Boolean = true,
) {
    /** The plates owned in the current [weightUnit]. */
    val currentPlates: List<Double> get() = plates[weightUnit].orEmpty()

    companion object {
        const val DEFAULT_REST_SECONDS = 90
        const val REST_STEP_SECONDS = 15
    }
}