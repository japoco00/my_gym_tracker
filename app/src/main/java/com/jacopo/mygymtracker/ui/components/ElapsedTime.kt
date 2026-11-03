package com.jacopo.mygymtracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.jacopo.mygymtracker.ui.format.formatDuration
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/** Time since [startedAt] as `m:ss` or `h:mm:ss`; recomposes the caller on every whole second. */
@Composable
fun rememberElapsedTime(startedAt: Instant): String {
    val elapsedMs by produceState(initialValue = millisSince(startedAt), startedAt) {
        while (true) {
            value = millisSince(startedAt)
            delay(1_000 - value % 1_000)
        }
    }
    return formatDuration(elapsedMs / 1_000)
}

private fun millisSince(instant: Instant): Long = Duration.between(instant, Instant.now()).toMillis()
