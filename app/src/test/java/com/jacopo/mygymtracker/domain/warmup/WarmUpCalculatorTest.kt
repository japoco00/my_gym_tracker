package com.jacopo.mygymtracker.domain.warmup

import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.SetType
import com.jacopo.mygymtracker.domain.model.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WarmUpCalculatorTest {

    @Test
    fun barbell_startsWithTheEmptyBarThenRampsUp() {
        assertEquals(
            listOf(warmUp(10, 0.0), warmUp(5, 40.0), warmUp(3, 60.0), warmUp(2, 80.0)),
            warmUpSets(100.0, isBarbell = true, WeightUnit.KG),
        )
    }

    @Test
    fun otherExercises_skipTheEmptyBar() {
        assertEquals(
            listOf(warmUp(5, 40.0), warmUp(3, 60.0), warmUp(2, 80.0)),
            warmUpSets(100.0, isBarbell = false, WeightUnit.KG),
        )
    }

    @Test
    fun roundsDownToALoadableStep() {
        // 40%, 60% and 80% of 85 kg are 34, 51 and 68.
        assertEquals(listOf(32.5, 50.0, 67.5), warmUpSets(85.0, isBarbell = false, WeightUnit.KG).map { it.weightKg })
    }

    @Test
    fun poundsRoundToPoundSteps() {
        val unit = WeightUnit.LB
        val weights = warmUpSets(unit.toKg(225.0), isBarbell = false, unit).map { unit.fromKg(it.weightKg) }

        // 40%, 60% and 80% of 225 lb are 90, 135 and 180: exact despite being stored as kilograms.
        assertEquals(3, weights.size)
        listOf(90.0, 135.0, 180.0).zip(weights).forEach { (expected, actual) -> assertEquals(expected, actual, 1e-9) }
    }

    @Test
    fun light_dropsRampsThatRoundToNothingOrRepeat() {
        // 40%, 60% and 80% of 5 kg are 2, 3 and 4: nothing, then 2.5 twice.
        assertEquals(listOf(warmUp(3, 2.5)), warmUpSets(5.0, isBarbell = false, WeightUnit.KG))
    }

    @Test
    fun noWorkingWeight_leavesOnlyTheEmptyBar() {
        assertEquals(listOf(warmUp(10, 0.0)), warmUpSets(0.0, isBarbell = true, WeightUnit.KG))
        assertTrue(warmUpSets(0.0, isBarbell = false, WeightUnit.KG).isEmpty())
    }

    private fun warmUp(reps: Int, weightKg: Double) = LoggedSet(reps = reps, weightKg = weightKg, type = SetType.WARM_UP)
}
