package com.jacopo.mygymtracker.ui.progress

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.progress.MetricQuantity
import com.jacopo.mygymtracker.domain.progress.ProgressMetric
import com.jacopo.mygymtracker.domain.progress.SessionProgress
import com.jacopo.mygymtracker.ui.format.formatDuration
import com.jacopo.mygymtracker.ui.format.shortDateFormatter
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.point
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.text.DecimalFormat
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private val SessionDatesKey = ExtraStore.Key<List<Instant>>()

/**
 * Line chart of [metric] over [sessions], one evenly spaced point per workout, labelled with its date.
 * Scrolls horizontally when the workouts don't fit and starts at the latest one.
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
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(sessions, metric, weightUnit) {
        modelProducer.runTransaction {
            lineSeries { series(sessions.map { metric.chartValue(checkNotNull(it.valueOf(metric)), weightUnit) }) }
            extras { it[SessionDatesKey] = sessions.map(SessionProgress::performedAt) }
        }
    }

    val dateFormatter = remember { shortDateFormatter() }
    val pointColor = MaterialTheme.colorScheme.primary
    ProvideVicoTheme(rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(
                            pointProvider = LineCartesianLayer.PointProvider.single(
                                LineCartesianLayer.point(
                                    component = rememberShapeComponent(fill(pointColor), CorneredShape.Pill),
                                ),
                            ),
                        ),
                    ),
                    pointSpacing = 56.dp,
                    rangeProvider = FitDataRangeProvider,
                ),
                startAxis = VerticalAxis.rememberStart(
                    valueFormatter = remember(metric.quantity) {
                        if (metric.quantity == MetricQuantity.SECONDS) {
                            CartesianValueFormatter { _, value, _ -> formatDuration(value.toLong()) }
                        } else {
                            CartesianValueFormatter.decimal(DecimalFormat("#.#"))
                        }
                    },
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = remember(dateFormatter) {
                        CartesianValueFormatter { context, x, _ ->
                            dateFormatter.format(context.model.extraStore[SessionDatesKey][x.toInt()])
                        }
                    },
                ),
            ),
            modelProducer = modelProducer,
            scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End),
            modifier = modifier,
        )
    }
}

/** Weights in the user's unit; reps and seconds as they are. */
private fun ProgressMetric.chartValue(value: Double, weightUnit: WeightUnit): Double = when (quantity) {
    MetricQuantity.WEIGHT_KG, MetricQuantity.VOLUME_KG -> weightUnit.fromKg(value)
    MetricQuantity.REPS, MetricQuantity.SECONDS -> value
}

/**
 * Fits the y-axis to the data with some headroom instead of starting at zero, so progress stays visible.
 * Bounds are multiples of the vertical axis' default label step, so labels land on round values.
 */
private object FitDataRangeProvider : CartesianLayerRangeProvider {
    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        val step = labelStep(maxY)
        return (floor(minY * 0.9 / step) * step).coerceAtLeast(0.0)
    }

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double {
        val step = labelStep(maxY)
        return ceil(maxY * 1.1 / step) * step
    }

    /** A tenth of the padded maximum's order of magnitude, e.g. 10 for 120. */
    private fun labelStep(maxY: Double): Double = 10.0.pow(floor(log10(maxY * 1.1)) - 1)
}
