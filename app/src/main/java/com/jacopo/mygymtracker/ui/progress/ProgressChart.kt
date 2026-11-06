package com.jacopo.mygymtracker.ui.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.progress.MetricQuantity
import com.jacopo.mygymtracker.domain.progress.ProgressMetric
import com.jacopo.mygymtracker.domain.progress.SessionProgress
import com.jacopo.mygymtracker.domain.progress.recordFor
import com.jacopo.mygymtracker.ui.components.DateLineChart

/**
 * Chart of [metric] over [sessions], one point per workout, the record one highlighted; see [DateLineChart].
 *
 * @param sessions oldest first; each must have a positive value for [metric].
 */
@Composable
fun ProgressChart(
    sessions: List<SessionProgress>,
    metric: ProgressMetric,
    weightUnit: WeightUnit,
    modifier: Modifier = Modifier,
) {
    val values = remember(sessions, metric, weightUnit) {
        sessions.map { metric.chartValue(checkNotNull(it.valueOf(metric)), weightUnit) }
    }
    val recordIndex = remember(sessions, metric) { sessions.recordFor(metric)?.let(sessions::indexOf) ?: -1 }
    DateLineChart(
        values = values,
        dates = remember(sessions) { sessions.map(SessionProgress::performedAt) },
        highlightIndex = recordIndex,
        formatsDuration = metric.quantity == MetricQuantity.SECONDS,
        modifier = modifier,
    )
}

/** Weights in the user's unit; reps and seconds as they are. */
private fun ProgressMetric.chartValue(value: Double, weightUnit: WeightUnit): Double = when (quantity) {
    MetricQuantity.WEIGHT_KG, MetricQuantity.VOLUME_KG -> weightUnit.fromKg(value)
    MetricQuantity.REPS, MetricQuantity.SECONDS -> value
}