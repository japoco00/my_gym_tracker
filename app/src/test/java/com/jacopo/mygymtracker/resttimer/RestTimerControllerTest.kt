package com.jacopo.mygymtracker.resttimer

import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.WeightUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerControllerTest {

    private var started = 0
    private var finished = 0
    private var lastFinished: RestTimer? = null

    private fun TestScope.controller() = RestTimerController(
        scope = backgroundScope,
        onRestStarted = { started++ },
        onRestFinished = {
            finished++
            lastFinished = it
        },
        clock = { testScheduler.currentTime },
    )

    @Test
    fun start_runsUntilTheEnd_thenFinishes() = runTest {
        val timer = controller()

        timer.start(90)
        assertEquals(RestTimer(endsAtMillis = 90_000, totalSeconds = 90), timer.state.value)
        assertEquals(1, started)

        advanceTimeBy(89_999)
        assertNotNull(timer.state.value)
        assertEquals(0, finished)

        advanceTimeBy(2)
        assertNull(timer.state.value)
        assertEquals(1, finished)
    }

    @Test
    fun start_whileRunning_restartsWithoutStartingAgain() = runTest {
        val timer = controller()

        timer.start(90)
        advanceTimeBy(60_000)
        timer.start(90)

        assertEquals(1, started)
        advanceTimeBy(89_999)
        assertNotNull(timer.state.value)
        advanceTimeBy(2)
        assertNull(timer.state.value)
        assertEquals(1, finished)
    }

    @Test
    fun adjust_extendsTheRest() = runTest {
        val timer = controller()

        timer.start(90)
        timer.adjust(15)

        assertEquals(RestTimer(endsAtMillis = 105_000, totalSeconds = 105), timer.state.value)
        advanceTimeBy(100_000)
        assertNotNull(timer.state.value)
        advanceTimeBy(5_001)
        assertNull(timer.state.value)
        assertEquals(1, finished)
    }

    @Test
    fun adjust_removingAllTime_endsWithoutFinishing() = runTest {
        val timer = controller()

        timer.start(90)
        advanceTimeBy(80_000)
        timer.adjust(-15)

        assertNull(timer.state.value)
        advanceTimeBy(60_000)
        assertEquals(0, finished)
    }

    @Test
    fun adjust_whenIdle_doesNothing() = runTest {
        val timer = controller()

        timer.adjust(15)

        assertNull(timer.state.value)
        assertEquals(0, started)
    }

    @Test
    fun adjust_keepsTheNextSet_untilTheFinishGetsIt() = runTest {
        val timer = controller()
        val next = NextSet(
            routineName = "Push",
            exercise = Exercise(name = "Squat", category = Category.CHEST),
            set = LoggedSet(reps = 5, weightKg = 100.0),
            number = 2,
            normalSets = 2,
            isLast = true,
            weightUnit = WeightUnit.KG,
        )

        timer.start(90, next)
        timer.adjust(-15)
        assertEquals(next, timer.state.value?.nextSet)

        advanceTimeBy(75_001)
        assertEquals(next, lastFinished?.nextSet)
    }

    @Test
    fun skip_endsWithoutFinishing() = runTest {
        val timer = controller()

        timer.start(90)
        timer.skip()

        assertNull(timer.state.value)
        advanceTimeBy(120_000)
        assertEquals(0, finished)
    }
}
