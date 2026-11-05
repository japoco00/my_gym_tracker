package com.jacopo.mygymtracker.domain.plates

import com.jacopo.mygymtracker.domain.model.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class PlateCalculatorTest {

    @Test
    fun splitsHalfTheWeightHeaviestFirst() {
        assertEquals(
            PlateBreakdown(listOf(25.0, 20.0, 2.5, 1.25), remainder = 0.0),
            platesPerSide(97.5, WeightUnit.KG.defaultPlates),
        )
    }

    @Test
    fun repeatsAPlateAsOftenAsNeeded() {
        assertEquals(listOf(25.0, 25.0, 25.0, 25.0), platesPerSide(200.0, WeightUnit.KG.defaultPlates).plates)
    }

    @Test
    fun reportsWhatThePlatesCannotMakeUpPerSide() {
        val breakdown = platesPerSide(61.0, listOf(20.0, 10.0, 2.5))

        assertEquals(listOf(20.0, 10.0), breakdown.plates)
        assertEquals(0.5, breakdown.remainder, 1e-9)
    }

    @Test
    fun poundsStoredAsKilogramsStillComeOutExact() {
        val total = WeightUnit.LB.fromKg(WeightUnit.LB.toKg(135.0))

        assertEquals(
            PlateBreakdown(listOf(45.0, 10.0, 10.0, 2.5), remainder = 0.0),
            platesPerSide(total, WeightUnit.LB.defaultPlates),
        )
    }

    @Test
    fun noPlates_leavesEverythingAsRemainder() {
        assertEquals(PlateBreakdown(emptyList(), remainder = 30.0), platesPerSide(60.0, emptyList()))
    }
}
