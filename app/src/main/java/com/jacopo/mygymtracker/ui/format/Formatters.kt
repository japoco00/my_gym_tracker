package com.jacopo.mygymtracker.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.WeightUnit
import java.util.Locale

/** `m:ss`, or `h:mm:ss` from one hour on. */
fun formatDuration(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = seconds % 3600 / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, secs)
    }
}

@Composable
fun WeightUnit.label(): String = stringResource(
    when (this) {
        WeightUnit.KG -> R.string.unit_kg
        WeightUnit.LB -> R.string.unit_lb
    },
)
