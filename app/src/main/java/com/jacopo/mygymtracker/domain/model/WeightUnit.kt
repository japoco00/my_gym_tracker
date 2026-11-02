package com.jacopo.mygymtracker.domain.model

import java.util.Locale

/**
 * Unit used to display and enter weights. Weights are always stored in kilograms.
 *
 * @property step increment used by weight steppers, expressed in this unit.
 */
enum class WeightUnit(private val kgPerUnit: Double, val step: Double) {
    KG(kgPerUnit = 1.0, step = 2.5),
    LB(kgPerUnit = 0.45359237, step = 5.0),
    ;

    fun fromKg(kg: Double): Double = kg / kgPerUnit

    fun toKg(value: Double): Double = value * kgPerUnit

    /** Formats a stored kilogram value in this unit, e.g. `80`, `82.5`, `61.23`. */
    fun format(kg: Double): String = formatWeight(fromKg(kg))
}

/** At most two decimals, no trailing zeros, always `.` as decimal separator. */
fun formatWeight(value: Double): String =
    String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')

/** Parses a user-typed weight, accepting `,` or `.` as separator. Returns `null` if invalid or negative. */
fun parseWeight(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }