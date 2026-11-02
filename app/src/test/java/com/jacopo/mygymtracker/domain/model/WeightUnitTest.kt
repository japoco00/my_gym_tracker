package com.jacopo.mygymtracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightUnitTest {

    @Test
    fun formatWeight_trimsTrailingZerosAndRoundsToTwoDecimals() {
        assertEquals("80", formatWeight(80.0))
        assertEquals("82.5", formatWeight(82.5))
        assertEquals("61.23", formatWeight(61.234))
        assertEquals("0", formatWeight(0.0))
        assertEquals("100", formatWeight(99.999))
    }

    @Test
    fun parseWeight_acceptsCommaAndDot() {
        assertEquals(82.5, parseWeight("82.5")!!, 0.0)
        assertEquals(82.5, parseWeight(" 82,5 ")!!, 0.0)
        assertEquals(0.0, parseWeight("0")!!, 0.0)
    }

    @Test
    fun parseWeight_rejectsInvalidInput() {
        assertNull(parseWeight(""))
        assertNull(parseWeight("abc"))
        assertNull(parseWeight("-5"))
        assertNull(parseWeight("NaN"))
    }

    @Test
    fun pounds_roundTripThroughKilograms() {
        val storedKg = WeightUnit.LB.toKg(135.0)

        assertEquals(61.235, storedKg, 0.001)
        assertEquals("135", WeightUnit.LB.format(storedKg))
        assertEquals("61.23", WeightUnit.KG.format(storedKg))
    }
}