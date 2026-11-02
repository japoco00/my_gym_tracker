package com.jacopo.mygymtracker.resttimer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A running rest that ends at [endsAtMillis] (wall clock) and lasts [totalSeconds] including adjustments. */
data class RestTimer(
    val endsAtMillis: Long,
    val totalSeconds: Int,
) {
    fun remainingMillis(nowMillis: Long): Long = (endsAtMillis - nowMillis).coerceAtLeast(0)
}

/**
 * The app-wide rest countdown. It lives in the app scope, so it keeps running when the
 * workout screen is left.
 *
 * @param onRestStarted called when a rest begins and none was running.
 * @param onRestFinished called when a rest runs out, not when it's skipped.
 */
class RestTimerController(
    private val scope: CoroutineScope,
    private val onRestStarted: () -> Unit = {},
    private val onRestFinished: () -> Unit = {},
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow<RestTimer?>(null)

    /** The running rest, or null when idle. */
    val state: StateFlow<RestTimer?> = _state.asStateFlow()

    private var expiryJob: Job? = null

    /** Starts a rest of [seconds], replacing the running one. */
    fun start(seconds: Int) {
        val wasIdle = _state.value == null
        run(RestTimer(endsAtMillis = clock() + seconds * 1_000L, totalSeconds = seconds))
        if (wasIdle) onRestStarted()
    }

    /** Adds time, or removes it with a negative [deltaSeconds]. Removing all of it ends the rest silently. */
    fun adjust(deltaSeconds: Int) {
        val timer = _state.value ?: return
        val adjusted = RestTimer(
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
            if (_state.compareAndSet(timer, null)) onRestFinished()
        }
    }
}
