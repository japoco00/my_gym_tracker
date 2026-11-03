package com.jacopo.mygymtracker.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.model.WorkoutSummary
import com.jacopo.mygymtracker.ui.format.formatDateTime
import com.jacopo.mygymtracker.ui.format.formatDuration
import com.jacopo.mygymtracker.ui.format.formatVolume
import com.jacopo.mygymtracker.ui.theme.AppTheme
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onWorkoutClick: (sessionId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }) },
        modifier = modifier,
    ) { innerPadding ->
        when {
            uiState.isLoading -> Unit
            uiState.workouts.isEmpty() -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
            ) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            else -> LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items = uiState.workouts, key = { it.id }) { workout ->
                    WorkoutRow(
                        workout = workout,
                        weightUnit = uiState.weightUnit,
                        modifier = Modifier
                            .animateItem()
                            .clickable { onWorkoutClick(workout.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutRow(
    workout: WorkoutSummary,
    weightUnit: WeightUnit,
    modifier: Modifier = Modifier,
) {
    val durationSeconds = Duration.between(workout.startedAt, workout.finishedAt).seconds
    ListItem(
        overlineContent = { Text(formatDateTime(workout.startedAt)) },
        supportingContent = {
            Text(
                stringResource(
                    R.string.history_workout_stats,
                    formatDuration(durationSeconds),
                    pluralStringResource(R.plurals.history_set_count, workout.setCount, workout.setCount),
                    weightUnit.formatVolume(workout.volumeKg),
                ),
            )
        },
        modifier = modifier,
    ) {
        Text(workout.routineName)
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    val now = Instant.now()
    AppTheme {
        HistoryScreen(
            uiState = HistoryUiState(
                workouts = listOf(
                    WorkoutSummary(
                        id = 2,
                        routineName = "Push Day",
                        startedAt = now.minusSeconds(3_600),
                        finishedAt = now,
                        setCount = 18,
                        volumeKg = 4_320.0,
                    ),
                    WorkoutSummary(
                        id = 1,
                        routineName = "Leg Day",
                        startedAt = now.minusSeconds(176_400),
                        finishedAt = now.minusSeconds(173_100),
                        setCount = 15,
                        volumeKg = 6_150.0,
                    ),
                ),
                isLoading = false,
            ),
            onWorkoutClick = {},
        )
    }
}
