package com.jacopo.mygymtracker.resttimer

import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.WeightUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A running rest that ends at [endsAtMillis] (wall clock) and lasts [totalSeconds] including adjustments.
 * [nextSet] is the set it leads to, for the notification, or null when none is left.
 */
data class RestTimer(
    val endsAtMillis: Long,
    val totalSeconds: Int,
    val nextSet: NextSet? = null,
) {
    fun remainingMillis(nowMillis: Long): Long = (endsAtMillis - nowMillis).coerceAtLeast(0)
}

/**
 * The set a rest leads to, as it was when the rest started.
 *
 * @property number the set's number among the exercise's normal sets, or null for the other types.
 * @property normalSets how many normal sets the exercise has.
 * @property isLast whether it's the only set of the workout left.
 */
data class NextSet(
    val routineName: String,
    val exercise: Exercise,
    val set: LoggedSet,
    val number: Int?,
    val normalSets: Int,
    val isLast: Boolean,
    val weightUnit: WeightUnit,
)

/**
 * The app-wide rest countdown. It lives in the app scope, so it keeps running when the
 * workout screen is left.
 *
 * @param onRestStarted called when a rest begins and none was running.
 * @param onRestFinished called with the rest that ran out, not when it's skipped.
 */
class RestTimerController(
    private val scope: CoroutineScope,
    private val onRestStarted: () -> Unit = {},
    private val onRestFinished: (RestTimer) -> Unit = {},
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow<RestTimer?>(null)

    /** The running rest, or null when idle. */
    val state: StateFlow<RestTimer?> = _state.asStateFlow()

    private var expiryJob: Job? = null

    /** Starts a rest of [seconds] before [nextSet], replacing the running one. */
    fun start(seconds: Int, nextSet: NextSet? = null) {
        val wasIdle = _state.value == null
        run(RestTimer(endsAtMillis = clock() + seconds * 1_000L, totalSeconds = seconds, nextSet = nextSet))
        if (wasIdle) onRestStarted()
    }

    /** Adds time, or removes it with a negative [deltaSeconds]. Removing all of it ends the rest silently. */
    fun adjust(deltaSeconds: Int) {
        val timer = _state.value ?: return
        val adjusted = timer.copy(
            endsAtMillis = timer.endsAtMillis + deltaSeconds * 1_000L,
            totalSeconds = timer.totalSeconds + deltaSeconds,
        )
        if (adjusted.endsAtMillis <= clock()) skip() else run(adjusted)
    }

    fun skip() {
        expiryJob?.cancel()
        _state.value = null
    }

    private fun run(timer: RestTimer) {
        expiryJob?.cancel()
        _state.value = timer
        expiryJob = scope.launch {
            delay(timer.endsAtMillis - clock())
            // Loses the race against a start or adjust made while this job was already past its delay.
            if (_state.compareAndSet(timer, null)) onRestFinished(timer)
        }
    }
}
