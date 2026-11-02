package com.jacopo.mygymtracker.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import com.jacopo.mygymtracker.ui.components.SwipeToDeleteContainer
import com.jacopo.mygymtracker.ui.theme.AppTheme
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    uiState: RoutinesUiState,
    onCreateRoutine: () -> Unit,
    onRoutineClick: (Int) -> Unit,
    onDeleteRoutine: (Routine) -> Unit,
    onUndoDelete: (routineId: Int) -> Unit,
    onConfirmDelete: (routineId: Int) -> Unit,
    onStartWorkout: (Routine) -> Unit,
    onResumeWorkout: (sessionId: Int) -> Unit,
    onDiscardActiveAndStart: () -> Unit,
    onDismissStartConflict: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    UndoDeleteSnackbar(
        pendingDelete = uiState.pendingDelete,
        snackbarHostState = snackbarHostState,
        onUndo = onUndoDelete,
        onConfirm = onConfirmDelete,
    )
    val activeWorkout = uiState.activeWorkout
    val startConflict = uiState.startConflict
    if (startConflict != null && activeWorkout != null) {
        StartConflictDialog(
            activeWorkout = activeWorkout,
            routine = startConflict,
            onResume = {
                onDismissStartConflict()
                activeWorkout.id?.let(onResumeWorkout)
            },
            onDiscardAndStart = onDiscardActiveAndStart,
            onDismiss = onDismissStartConflict,
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.routines_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateRoutine,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.routines_create)) },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        if (uiState.isLoading) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (activeWorkout != null) {
                ActiveWorkoutBanner(
                    routineName = activeWorkout.routineName,
                    onResume = { activeWorkout.id?.let(onResumeWorkout) },
                )
            }
            if (uiState.routines.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.routines_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(
                        items = uiState.routines,
                        key = { routine -> checkNotNull(routine.id) },
                    ) { routine ->
                        val isWorkoutActive = activeWorkout != null && activeWorkout.routineId == routine.id
                        SwipeToDeleteContainer(
                            onDelete = { onDeleteRoutine(routine) },
                            modifier = Modifier.animateItem(),
                        ) {
                            RoutineOverviewCard(
                                routine = routine,
                                isWorkoutActive = isWorkoutActive,
                                onClick = { routine.id?.let(onRoutineClick) },
                                onStartWorkout = {
                                    if (isWorkoutActive) {
                                        activeWorkout.id?.let(onResumeWorkout)
                                    } else {
                                        onStartWorkout(routine)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveWorkoutBanner(
    routineName: String,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.workout_in_progress),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = routineName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(onClick = onResume) { Text(stringResource(R.string.workout_resume)) }
        }
    }
}

/** Only one workout can run at a time: resume it, or throw it away and start [routine]. */
@Composable
private fun StartConflictDialog(
    activeWorkout: WorkoutSession,
    routine: Routine,
    onResume: () -> Unit,
    onDiscardAndStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workout_in_progress)) },
        text = {
            Text(stringResource(R.string.workout_conflict_message, activeWorkout.routineName, routine.name))
        },
        confirmButton = {
            TextButton(onClick = onResume) { Text(stringResource(R.string.workout_resume)) }
        },
        dismissButton = {
            TextButton(onClick = onDiscardAndStart) {
                Text(stringResource(R.string.workout_discard_and_start))
            }
        },
    )
}

/** Shows the undo snackbar for [pendingDelete]; a newer delete replaces the current snackbar. */
@Composable
private fun UndoDeleteSnackbar(
    pendingDelete: Routine?,
    snackbarHostState: SnackbarHostState,
    onUndo: (routineId: Int) -> Unit,
    onConfirm: (routineId: Int) -> Unit,
) {
    val message = stringResource(R.string.routine_deleted, pendingDelete?.name.orEmpty())
    val undoLabel = stringResource(R.string.action_undo)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val currentOnConfirm by rememberUpdatedState(onConfirm)

    LaunchedEffect(pendingDelete?.id) {
        val routineId = pendingDelete?.id ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) currentOnUndo(routineId) else currentOnConfirm(routineId)
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutinesScreenPreview() {
    AppTheme {
        RoutinesScreen(
            uiState = RoutinesUiState(
                routines = listOf(
                    Routine(id = 1, name = "Upper Body", note = "Monday"),
                    Routine(id = 2, name = "Lower Body", note = "Thursday"),
                ),
                activeWorkout = WorkoutSession(
                    id = 1,
                    routineId = 1,
                    routineName = "Upper Body",
                    startedAt = Instant.now(),
                ),
                isLoading = false,
            ),
            onCreateRoutine = {},
            onRoutineClick = {},
            onDeleteRoutine = {},
            onUndoDelete = {},
            onConfirmDelete = {},
            onStartWorkout = {},
            onResumeWorkout = {},
            onDiscardActiveAndStart = {},
            onDismissStartConflict = {},
        )
    }
}
