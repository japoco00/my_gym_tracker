package com.jacopo.mygymtracker.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.PlannedSet
import com.jacopo.mygymtracker.domain.model.Routine
import com.jacopo.mygymtracker.domain.model.RoutineExercise
import com.jacopo.mygymtracker.domain.model.WorkoutExercise
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import com.jacopo.mygymtracker.domain.model.WorkoutSummary
import com.jacopo.mygymtracker.ui.components.ActiveWorkoutCard
import com.jacopo.mygymtracker.ui.components.AppHeader
import com.jacopo.mygymtracker.ui.components.EmptyStateText
import com.jacopo.mygymtracker.ui.components.FloatingDockClearance
import com.jacopo.mygymtracker.ui.components.Pill
import com.jacopo.mygymtracker.ui.components.SectionLabel
import com.jacopo.mygymtracker.ui.components.StartConflictDialog
import com.jacopo.mygymtracker.ui.components.SwipeToDeleteContainer
import com.jacopo.mygymtracker.ui.components.sharedContainer
import com.jacopo.mygymtracker.ui.format.formatRelativeDay
import com.jacopo.mygymtracker.ui.theme.AppTheme
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    uiState: RoutinesUiState,
    onRoutineClick: (Int) -> Unit,
    onDeleteRoutine: (Routine) -> Unit,
    onUndoDelete: (routineId: Int) -> Unit,
    onConfirmDelete: (routineId: Int) -> Unit,
    onStartWorkout: (Routine) -> Unit,
    onStartEmptyWorkout: () -> Unit,
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            AppHeader(
                title = stringResource(R.string.routines_title),
                scrollBehavior = scrollBehavior,
                pills = { if (!uiState.isLoading) HeaderPills(uiState.routines.size, uiState.lastWorkout) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.padding(bottom = FloatingDockClearance)) },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        if (uiState.isLoading) return@Scaffold
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = FloatingDockClearance),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (activeWorkout != null) {
                item(key = "active") {
                    ActiveWorkoutCard(
                        workout = activeWorkout,
                        onResume = { activeWorkout.id?.let(onResumeWorkout) },
                        modifier = Modifier
                            .animateItem()
                            .padding(bottom = 16.dp),
                    )
                }
            }
            item(key = "empty-workout") {
                EmptyWorkoutItem(
                    onStart = onStartEmptyWorkout,
                    modifier = Modifier
                        .animateItem()
                        .padding(bottom = 16.dp),
                )
            }
            item(key = "section") {
                SectionLabel(
                    text = stringResource(R.string.routines_section),
                    modifier = Modifier.animateItem(),
                )
            }
            if (uiState.routines.isEmpty()) {
                item(key = "empty") {
                    EmptyStateText(
                        text = stringResource(R.string.routines_empty),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
            itemsIndexed(
                items = uiState.routines,
                key = { _, routine -> checkNotNull(routine.id) },
            ) { index, routine ->
                val isWorkoutActive = activeWorkout != null && activeWorkout.routineId == routine.id
                val shapes = ListItemDefaults.segmentedShapes(index = index, count = uiState.routines.size)
                SwipeToDeleteContainer(
                    onDelete = { onDeleteRoutine(routine) },
                    // Clipped so the red swipe background follows the row's rounded corners.
                    modifier = Modifier
                        .animateItem()
                        .sharedContainer(routineContainerKey(checkNotNull(routine.id)), shapes.shape)
                        .clip(shapes.shape),
                ) {
                    RoutineListItem(
                        routine = routine,
                        shapes = shapes,
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

@Composable
private fun HeaderPills(routineCount: Int, lastWorkout: WorkoutSummary?) {
    Pill(
        icon = painterResource(R.drawable.ic_fitness_center),
        text = pluralStringResource(R.plurals.routines_count, routineCount, routineCount),
    )
    if (lastWorkout != null) {
        Pill {
            Icon(
                painter = painterResource(R.drawable.ic_history),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(
                    R.string.routines_last_workout,
                    lastWorkout.routineName,
                    formatRelativeDay(lastWorkout.finishedAt),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Starts a workout with no routine behind it; styled as a one-row group like a routine row. */
@Composable
private fun EmptyWorkoutItem(onStart: () -> Unit, modifier: Modifier = Modifier) {
    SegmentedListItem(
        onClick = onStart,
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
        modifier = modifier,
        supportingContent = { Text(stringResource(R.string.routines_empty_workout_hint)) },
        trailingContent = {
            FilledTonalIconButton(onClick = onStart, shapes = IconButtonDefaults.shapes()) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.routines_empty_workout_start),
                )
            }
        },
    ) {
        Text(text = stringResource(R.string.routines_empty_workout), fontWeight = FontWeight.SemiBold)
    }
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
    val bench = Exercise(id = 1, name = "Bench Press", category = Category.CHEST)
    val press = Exercise(id = 2, name = "Overhead Press", category = Category.SHOULDERS)
    val now = Instant.now()
    AppTheme {
        RoutinesScreen(
            uiState = RoutinesUiState(
                routines = listOf(
                    Routine(
                        id = 1,
                        name = "Upper Body",
                        note = "Monday",
                        exercises = listOf(
                            RoutineExercise(exercise = bench, sets = List(3) { PlannedSet(reps = 8, weightKg = 80.0) }),
                            RoutineExercise(exercise = press, sets = List(3) { PlannedSet(reps = 10, weightKg = 40.0) }),
                        ),
                    ),
                    Routine(id = 2, name = "Lower Body", note = "Thursday"),
                ),
                lastWorkout = WorkoutSummary(
                    id = 1,
                    routineName = "Lower Body",
                    startedAt = now.minus(Duration.ofDays(1)),
                    finishedAt = now.minus(Duration.ofDays(1)),
                    setCount = 12,
                    volumeKg = 4320.0,
                ),
                activeWorkout = WorkoutSession(
                    id = 1,
                    routineId = 1,
                    routineName = "Upper Body",
                    startedAt = now.minus(Duration.ofMinutes(12)),
                    exercises = listOf(
                        WorkoutExercise(exercise = bench, sets = List(3) { LoggedSet(isCompleted = true) }),
                        WorkoutExercise(exercise = press, sets = List(3) { LoggedSet() }),
                    ),
                ),
                isLoading = false,
            ),
            onRoutineClick = {},
            onDeleteRoutine = {},
            onUndoDelete = {},
            onConfirmDelete = {},
            onStartWorkout = {},
            onStartEmptyWorkout = {},
            onResumeWorkout = {},
            onDiscardActiveAndStart = {},
            onDismissStartConflict = {},
        )
    }
}
