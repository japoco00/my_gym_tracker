package com.jacopo.mygymtracker.domain.warmup

import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.SetType
import com.jacopo.mygymtracker.domain.model.WeightUnit
import kotlin.math.floor

/** Fraction of the working weight and the reps of each warm-up set after the empty bar. */
private val RAMP = listOf(0.4 to 5, 0.6 to 3, 0.8 to 2)

private const val EMPTY_BAR_REPS = 10

/** Absorbs the drift of pounds stored as kilograms, so 90 lb doesn't round down to 85. */
private const val TOLERANCE = 1e-6

/**
 * Warm-up sets ramping up to [workingKg]: the empty bar for a barbell, then 40%, 60% and 80% of it.
 * Each weight rounds down to a [unit] step so it can be loaded; ramps that round to nothing or repeat are dropped.
 * Logged weights leave the bar out, so the empty bar weighs 0.
 */
fun warmUpSets(workingKg: Double, isBarbell: Boolean, unit: WeightUnit): List<LoggedSet> {
    val working = unit.fromKg(workingKg)
    val ramp = RAMP.mapNotNull { (fraction, reps) ->
        val weight = floor(working * fraction / unit.step + TOLERANCE) * unit.step
        if (weight > 0) LoggedSet(reps = reps, weightKg = unit.toKg(weight), type = SetType.WARM_UP) else null
    }.distinctBy { it.weightKg }
    val emptyBar = LoggedSet(reps = EMPTY_BAR_REPS, type = SetType.WARM_UP).takeIf { isBarbell }
    return listOfNotNull(emptyBar) + ramp
}
