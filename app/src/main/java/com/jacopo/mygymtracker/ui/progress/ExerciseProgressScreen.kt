package com.jacopo.mygymtracker.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.progress.MAX_ESTIMATE_REPS
import com.jacopo.mygymtracker.domain.progress.MetricQuantity
import com.jacopo.mygymtracker.domain.progress.ProgressMetric
import com.jacopo.mygymtracker.domain.progress.SessionProgress
import com.jacopo.mygymtracker.ui.components.AppHeader
import com.jacopo.mygymtracker.ui.components.BackButton
import com.jacopo.mygymtracker.ui.components.Pill
import com.jacopo.mygymtracker.ui.components.SectionLabel
import com.jacopo.mygymtracker.ui.components.ShapeBadge
import com.jacopo.mygymtracker.ui.format.formatDate
import com.jacopo.mygymtracker.ui.format.formatDuration
import com.jacopo.mygymtracker.ui.format.formatSet
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.theme.AppTheme
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressScreen(
    uiState: ExerciseProgressUiState,
    onBack: () -> Unit,
    onMetricSelect: (ProgressMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = { ProgressHeader(uiState = uiState, scrollBehavior = scrollBehavior, onBack = onBack) },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        when {
            uiState.isLoading -> Unit
            uiState.sessions.isEmpty() -> Text(
                text = stringResource(R.string.progress_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 28.dp, vertical = 16.dp),
            )
            else -> ProgressContent(
                uiState = uiState,
                onMetricSelect = onMetricSelect,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

/** The exercise's name over its category and workout count, with the metric's record in a sunny badge. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProgressHeader(
    uiState: ExerciseProgressUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: () -> Unit,
) {
    val workoutCount =
        pluralStringResource(R.plurals.history_workout_count, uiState.sessions.size, uiState.sessions.size)
    AppHeader(
        title = uiState.exerciseName,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            BackButton(onClick = onBack)
        },
        actions = {
            uiState.record?.valueOf(uiState.metric)?.let { best ->
                val (value, unit) = uiState.metric.valueAndUnit(best, uiState.weightUnit)
                ShapeBadge(
                    shape = MaterialShapes.Sunny,
                    value = value,
                    label = unit,
                    size = 64.dp,
                    contentDescription = stringResource(
                        R.string.progress_best_description,
                        uiState.metric.label(),
                        uiState.metric.format(best, uiState.weightUnit),
                    ),
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        },
        pills = {
            if (!uiState.isLoading) {
                uiState.category?.let { category -> Pill { Text(text = category.label()) } }
                Pill(icon = painterResource(R.drawable.ic_history), text = workoutCount)
            }
        },
        collapsedSubtitle = { Text(text = workoutCount) },
    )
}

@Composable
private fun ProgressContent(
    uiState: ExerciseProgressUiState,
    onMetricSelect: (ProgressMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sessions = uiState.sessions.asReversed()
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "metrics") {
            MetricToggles(metrics = uiState.metrics, selected = uiState.metric, onSelect = onMetricSelect)
        }
        item(key = "chart") {
            ChartCard(uiState = uiState, modifier = Modifier.padding(top = 14.dp, bottom = 22.dp))
        }
        item(key = "sessions") {
            SectionLabel(text = stringResource(R.string.progress_sessions))
        }
        itemsIndexed(items = sessions, key = { _, session -> session.sessionId }) { index, session ->
            SessionListItem(
                session = session,
                uiState = uiState,
                shapes = ListItemDefaults.segmentedShapes(index = index, count = sessions.size),
            )
        }
    }
}

/** The metrics as a connected button group, with exactly one selected. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MetricToggles(
    metrics: List<ProgressMetric>,
    selected: ProgressMetric,
    onSelect: (ProgressMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        modifier = modifier.fillMaxWidth(),
    ) {
        metrics.forEachIndexed { index, metric ->
            ToggleButton(
                checked = metric == selected,
                onCheckedChange = { onSelect(metric) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    metrics.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
            ) {
                Text(text = metric.label(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** The metric's description over its chart. */
@Composable
private fun ChartCard(uiState: ExerciseProgressUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.extraLarge)
            .padding(16.dp),
    ) {
        Text(
            text = uiState.metric.description(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (uiState.chartSessions.isEmpty()) {
            Text(
                text = stringResource(R.string.progress_no_chart_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            ProgressChart(
                sessions = uiState.chartSessions,
                metric = uiState.metric,
                weightUnit = uiState.weightUnit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(200.dp),
            )
        }
    }
}

/** A workout's value for the metric over its best set; the record's is tinted and starred. */
@Composable
private fun SessionListItem(
    session: SessionProgress,
    uiState: ExerciseProgressUiState,
    shapes: ListItemShapes,
    modifier: Modifier = Modifier,
) {
    val isRecord = session.sessionId == uiState.record?.sessionId
    SegmentedListItem(
        shapes = shapes,
        overlineContent = { Text(text = formatDate(session.performedAt)) },
        supportingContent = {
            session.bestSet?.let { set ->
                val best =
                    formatSet(uiState.trackingType, uiState.weightUnit, set.reps, set.weightKg, set.durationSeconds)
                Text(text = stringResource(R.string.progress_best_set, best))
            }
        },
        trailingContent = if (isRecord) {
            {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = stringResource(R.string.progress_personal_record),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            null
        },
        colors = if (isRecord) {
            ListItemDefaults.segmentedColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                overlineContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                supportingContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        } else {
            ListItemDefaults.segmentedColors()
        },
        modifier = modifier,
    ) {
        Text(
            text = uiState.metric.format(session.valueOf(uiState.metric), uiState.weightUnit),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** [value] with its unit, e.g. `82.5 kg`, `12 reps` or `1:30 min`; `–` without a positive value. */
@Composable
private fun ProgressMetric.format(value: Double?, weightUnit: WeightUnit): String =
    if (value == null || value <= 0) {
        stringResource(R.string.progress_no_value)
    } else {
        val (number, unit) = valueAndUnit(value, weightUnit)
        stringResource(R.string.weight_with_unit, number, unit)
    }

/** [value] and its unit apart: weights with decimals, volume in whole units, durations as `m:ss`. */
@Composable
private fun ProgressMetric.valueAndUnit(value: Double, weightUnit: WeightUnit): Pair<String, String> =
    when (quantity) {
        MetricQuantity.WEIGHT_KG -> weightUnit.format(value) to weightUnit.label()
        MetricQuantity.VOLUME_KG -> weightUnit.fromKg(value).roundToLong().toString() to weightUnit.label()
        MetricQuantity.REPS -> value.toInt().let { reps ->
            reps.toString() to pluralStringResource(R.plurals.progress_unit_reps, reps)
        }
        MetricQuantity.SECONDS -> formatDuration(value.toLong()) to stringResource(
            if (value >= 3_600) R.string.progress_unit_hours else R.string.progress_unit_minutes,
        )
    }

@Composable
private fun ProgressMetric.label(): String = stringResource(
    when (this) {
        ProgressMetric.ESTIMATED_ONE_REP_MAX -> R.string.progress_metric_e1rm
        ProgressMetric.HEAVIEST_WEIGHT -> R.string.progress_metric_heaviest
        ProgressMetric.SESSION_VOLUME -> R.string.progress_metric_volume
        ProgressMetric.MOST_REPS -> R.string.progress_metric_most_reps
        ProgressMetric.TOTAL_REPS -> R.string.progress_metric_total_reps
        ProgressMetric.LONGEST_SET -> R.string.progress_metric_longest
        ProgressMetric.TOTAL_DURATION -> R.string.progress_metric_total_duration
    },
)

@Composable
private fun ProgressMetric.description(): String = when (this) {
    ProgressMetric.ESTIMATED_ONE_REP_MAX ->
        pluralStringResource(R.plurals.progress_metric_e1rm_description, MAX_ESTIMATE_REPS, MAX_ESTIMATE_REPS)
    ProgressMetric.HEAVIEST_WEIGHT -> stringResource(R.string.progress_metric_heaviest_description)
    ProgressMetric.SESSION_VOLUME -> stringResource(R.string.progress_metric_volume_description)
    ProgressMetric.MOST_REPS -> stringResource(R.string.progress_metric_most_reps_description)
    ProgressMetric.TOTAL_REPS -> stringResource(R.string.progress_metric_total_reps_description)
    ProgressMetric.LONGEST_SET -> stringResource(R.string.progress_metric_longest_description)
    ProgressMetric.TOTAL_DURATION -> stringResource(R.string.progress_metric_total_duration_description)
}

@Preview(showBackground = true)
@Composable
private fun ExerciseProgressScreenPreview() {
    val now = Instant.now()
    val sessions = listOf(80.0, 85.0, 82.5).mapIndexed { index, heaviestKg ->
        SessionProgress(
            sessionId = index,
            performedAt = now.minus(Duration.ofDays(7L * (2 - index))),
            heaviestKg = heaviestKg,
            estimatedOneRepMaxKg = heaviestKg * (1 + 8 / 30.0),
            volumeKg = heaviestKg * 8 * 3,
            mostReps = 8,
            totalReps = 24,
            longestSeconds = 0,
            totalSeconds = 0,
            bestSet = LoggedSet(reps = 8, weightKg = heaviestKg),
        )
    }
    AppTheme {
        ExerciseProgressScreen(
            uiState = ExerciseProgressUiState(
                exerciseName = "Bench Press",
                category = Category.CHEST,
                sessions = sessions,
                isLoading = false,
            ),
            onBack = {},
            onMetricSelect = {},
        )
    }
}