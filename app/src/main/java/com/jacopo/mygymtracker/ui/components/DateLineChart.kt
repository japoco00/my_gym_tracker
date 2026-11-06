package com.jacopo.mygymtracker.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.text.DecimalFormat
import java.time.temporal.TemporalAccessor
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private val DatesKey = ExtraStore.Key<List<TemporalAccessor>>()
private val HighlightIndexKey = ExtraStore.Key<Int>()

/**
 * Line chart of [values], one evenly spaced point per entry, labelled with the matching entry of [dates].
 * Scrolls horizontally when the points don't fit and starts at the latest one.
 *
 * @param values oldest first, all positive.
 * @param dates instants or local dates, one per value.
 * @param highlightIndex the point drawn bigger with a halo, e.g. a record; -1 for none.
 * @param formatsDuration labels the y-axis as durations, the values being seconds.
 */
@Composable
fun DateLineChart(
    values: List<Double>,
    dates: List<TemporalAccessor>,
    highlightIndex: Int,
    formatsDuration: Boolean,
    modifier: Modifier = Modifier,
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values, dates, highlightIndex) {
        modelProducer.runTransaction {
            lineSeries { series(values) }
            extras {
                it[DatesKey] = dates
                it[HighlightIndexKey] = highlightIndex
            }
        }
    }

    val dateFormatter = remember { shortDateFormatter() }
    val colors = MaterialTheme.colorScheme
    val regularPoint = LineCartesianLayer.point(rememberShapeComponent(fill(colors.primary), CorneredShape.Pill))
    val highlightPoint = LineCartesianLayer.point(
        component = rememberShapeComponent(
            fill = fill(colors.primary),
            shape = CorneredShape.Pill,
            strokeFill = fill(colors.primaryContainer),
            strokeThickness = 4.dp,
        ),
        size = 16.dp,
    )
    ProvideVicoTheme(rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(
                            pointProvider = remember(regularPoint, highlightPoint) {
                                HighlightPointProvider(regular = regularPoint, highlight = highlightPoint)
                            },
                        ),
                    ),
                    pointSpacing = 56.dp,
                    rangeProvider = FitDataRangeProvider,
                ),
                startAxis = VerticalAxis.rememberStart(
                    valueFormatter = remember(formatsDuration) {
                        if (formatsDuration) {
                            CartesianValueFormatter { _, value, _ -> formatDuration(value.toLong()) }
                        } else {
                            CartesianValueFormatter.decimal(DecimalFormat("#.#"))
                        }
                    },
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = remember(dateFormatter) {
                        CartesianValueFormatter { context, x, _ ->
                            dateFormatter.format(context.model.extraStore[DatesKey][x.toInt()])
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

/** Draws the point at [HighlightIndexKey] bigger and haloed. */
private data class HighlightPointProvider(
    private val regular: LineCartesianLayer.Point,
    private val highlight: LineCartesianLayer.Point,
) : LineCartesianLayer.PointProvider {
    override fun getPoint(
        entry: LineCartesianLayerModel.Entry,
        seriesIndex: Int,
        extraStore: ExtraStore,
    ): LineCartesianLayer.Point = if (entry.x.toInt() == extraStore.getOrNull(HighlightIndexKey)) highlight else regular

    override fun getLargestPoint(extraStore: ExtraStore): LineCartesianLayer.Point = highlight
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
