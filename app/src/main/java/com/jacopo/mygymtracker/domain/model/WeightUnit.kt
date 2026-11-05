package com.jacopo.mygymtracker.domain.model

import java.util.Locale

/**
 * Unit used to display and enter weights. Weights are always stored in kilograms.
 *
 * @property step increment used by weight steppers, expressed in this unit.
 * @property plateOptions plate sizes the settings offer, heaviest first.
 * @property defaultPlates the plates a typical gym has, owned until the user picks their own.
 */
enum class WeightUnit(
    private val kgPerUnit: Double,
    val step: Double,
    val plateOptions: List<Double>,
    val defaultPlates: List<Double>,
) {
    KG(
        kgPerUnit = 1.0,
        step = 2.5,
        plateOptions = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 2.0, 1.25, 1.0, 0.5),
        defaultPlates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25),
    ),
    LB(
        kgPerUnit = 0.45359237,
        step = 5.0,
        plateOptions = listOf(55.0, 45.0, 35.0, 25.0, 10.0, 5.0, 2.5, 1.25),
        defaultPlates = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5),
    ),
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