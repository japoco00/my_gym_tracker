package com.jacopo.mygymtracker.domain.plates

/**
 * The plates to load on each side of the bar, heaviest first.
 *
 * @property remainder the weight per side the plates can't make up; 0 when exact.
 */
data class PlateBreakdown(val plates: List<Double>, val remainder: Double)

/**
 * Splits half of [total] into the [available] plates, each usable any number of times.
 * The bar weighs nothing here: the whole total is plates. Both share one unit.
 *
 * Greedy, heaviest first, like loading by hand: exact for the usual plate sets.
 */
fun platesPerSide(total: Double, available: List<Double>): PlateBreakdown {
    var left = total / 2
    val plates = buildList {
        for (plate in available.filter { it > 0 }.sortedDescending()) {
            // The tolerance absorbs the drift of pounds stored as kilograms.
            while (left >= plate - TOLERANCE) {
                add(plate)
                left -= plate
            }
        }
    }
    return PlateBreakdown(plates, remainder = if (left > TOLERANCE) left else 0.0)
}

private const val TOLERANCE = 1e-6
