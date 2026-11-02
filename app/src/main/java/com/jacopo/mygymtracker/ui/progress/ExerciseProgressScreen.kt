package com.jacopo.mygymtracker.ui.progress

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.progress.MAX_ESTIMATE_REPS
import com.jacopo.mygymtracker.domain.progress.PersonalRecord
import com.jacopo.mygymtracker.domain.progress.ProgressMetric
import com.jacopo.mygymtracker.domain.progress.SessionProgress
import com.jacopo.mygymtracker.domain.progress.personalRecords
import com.jacopo.mygymtracker.ui.format.formatDate
import com.jacopo.mygymtracker.ui.format.formatDateTime
import com.jacopo.mygymtracker.ui.format.formatVolume
import com.jacopo.mygymtracker.ui.format.label
import com.jacopo.mygymtracker.ui.theme.AppTheme
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressScreen(
    uiState: ExerciseProgressUiState,
    onBack: () -> Unit,
    onMetricSelect: (ProgressMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.exerciseName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        when {
            uiState.isLoading -> Unit
            uiState.sessions.isEmpty() -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
            ) {
                Text(
                    text = stringResource(R.string.progress_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            else -> ProgressContent(uiState = uiState, onMetricSelect = onMetricSelect, contentPadding = innerPadding)
        }
    }
}

@Composable
private fun ProgressContent(
    uiState: ExerciseProgressUiState,
    onMetricSelect: (ProgressMetric) -> Unit,
    contentPadding: PaddingValues,
) {
    val record = uiState.records[uiState.metric]
    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            PersonalRecords(
                records = uiState.records,
                weightUnit = uiState.weightUnit,
                modifier = Modifier.padding(16.dp),
            )
        }
        item {
            MetricSelector(
                selected = uiState.metric,
                onSelect = onMetricSelect,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item {
            Text(
                text = uiState.metric.description(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        item {
            if (uiState.chartSessions.isEmpty()) {
                Text(
                    text = stringResource(R.string.progress_no_chart_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            } else {
                ProgressChart(
                    sessions = uiState.chartSessions,
                    metric = uiState.metric,
                    weightUnit = uiState.weightUnit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(horizontal = 16.dp),
                )
            }
        }
        item {
            Text(
                text = stringResource(R.string.progress_workouts),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp),
            )
        }
        items(items = uiState.sessions.asReversed(), key = { it.sessionId }) { session ->
            ListItem(
                headlineContent = { Text(uiState.weightUnit.formatMetric(uiState.metric, session.valueOf(uiState.metric))) },
                supportingContent = { Text(formatDateTime(session.performedAt)) },
                trailingContent = {
                    if (record?.achievedAt == session.performedAt) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = stringResource(R.string.progress_personal_record),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun PersonalRecords(
    records: Map<ProgressMetric, PersonalRecord>,
    weightUnit: WeightUnit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.progress_records), style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.padding(top = 12.dp)) {
                ProgressMetric.entries.forEach { metric ->
                    val record = records[metric]
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = metric.label(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = weightUnit.formatMetric(metric, record?.valueKg),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (record != null) {
                            Text(
                                text = formatDate(record.achievedAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricSelector(
    selected: ProgressMetric,
    onSelect: (ProgressMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        ProgressMetric.entries.forEachIndexed { index, metric ->
            SegmentedButton(
                selected = metric == selected,
                onClick = { onSelect(metric) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ProgressMetric.entries.size),
                label = { Text(metric.label()) },
            )
        }
    }
}

/** Weights with decimals, volume in whole units, `–` without a positive value. */
@Composable
private fun WeightUnit.formatMetric(metric: ProgressMetric, valueKg: Double?): String = when {
    valueKg == null || valueKg <= 0 -> stringResource(R.string.progress_no_value)
    metric == ProgressMetric.SESSION_VOLUME -> formatVolume(valueKg)
    else -> stringResource(R.string.weight_with_unit, format(valueKg), label())
}

@Composable
private fun ProgressMetric.label(): String = stringResource(
    when (this) {
        ProgressMetric.ESTIMATED_ONE_REP_MAX -> R.string.progress_metric_e1rm
        ProgressMetric.HEAVIEST_WEIGHT -> R.string.progress_metric_heaviest
        ProgressMetric.SESSION_VOLUME -> R.string.progress_metric_volume
    },
)

@Composable
private fun ProgressMetric.description(): String = when (this) {
    ProgressMetric.ESTIMATED_ONE_REP_MAX ->
        pluralStringResource(R.plurals.progress_metric_e1rm_description, MAX_ESTIMATE_REPS, MAX_ESTIMATE_REPS)
    ProgressMetric.HEAVIEST_WEIGHT -> stringResource(R.string.progress_metric_heaviest_description)
    ProgressMetric.SESSION_VOLUME -> stringResource(R.string.progress_metric_volume_description)
}

@Preview(showBackground = true)
@Composable
private fun ExerciseProgressScreenPreview() {
    val now = Instant.now()
    val sessions = listOf(80.0, 82.5, 85.0).mapIndexed { index, heaviestKg ->
        SessionProgress(
            sessionId = index,
            performedAt = now.minus(Duration.ofDays(7L * (2 - index))),
            heaviestKg = heaviestKg,
            estimatedOneRepMaxKg = heaviestKg * (1 + 8 / 30.0),
            volumeKg = heaviestKg * 8 * 3,
        )
    }
    AppTheme {
        ExerciseProgressScreen(
            uiState = ExerciseProgressUiState(
                exerciseName = "Bench Press",
                sessions = sessions,
                records = sessions.personalRecords(),
                isLoading = false,
            ),
            onBack = {},
            onMetricSelect = {},
        )
    }
}
