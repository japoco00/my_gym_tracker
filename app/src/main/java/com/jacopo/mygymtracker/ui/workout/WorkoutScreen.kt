package com.jacopo.mygymtracker.ui.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.UserSettings
import com.jacopo.mygymtracker.resttimer.RestTimer
import com.jacopo.mygymtracker.ui.components.AddExercisesSheet
import com.jacopo.mygymtracker.ui.format.formatDuration
import com.jacopo.mygymtracker.ui.theme.AppTheme
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    uiState: WorkoutUiState,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onOpenAddExercises: () -> Unit,
    onDismissAddExercises: () -> Unit,
    onConfirmAddExercises: (List<Exercise>) -> Unit,
    onRemoveExercise: (exerciseKey: String) -> Unit,
    onAddSet: (exerciseKey: String) -> Unit,
    onDeleteSet: (exerciseKey: String, setKey: String) -> Unit,
    onRepsChange: (exerciseKey: String, setKey: String, Int) -> Unit,
    onWeightChange: (exerciseKey: String, setKey: String, Double) -> Unit,
    onSetCompletedChange: (exerciseKey: String, setKey: String, completed: Boolean) -> Unit,
    onAdjustRest: (deltaSeconds: Int) -> Unit,
    onSkipRest: () -> Unit,
    onConfirmFinish: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onConfirmRemoveExercise: (exerciseKey: String) -> Unit,
    onResolveRoutineUpdate: (update: Boolean) -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WorkoutDialogHost(
        dialog = uiState.dialog,
        routineName = uiState.routineName,
        onConfirmFinish = onConfirmFinish,
        onConfirmDiscard = onConfirmDiscard,
        onConfirmRemoveExercise = onConfirmRemoveExercise,
        onResolveRoutineUpdate = onResolveRoutineUpdate,
        onDismiss = onDismissDialog,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = uiState.routineName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!uiState.isLoading) ElapsedTime(startedAt = uiState.startedAt)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onFinish, enabled = !uiState.isLoading) {
                        Text(text = stringResource(R.string.workout_finish))
                    }
                },
            )
        },
        bottomBar = {
            uiState.restTimer?.let { timer ->
                RestTimerBar(timer = timer, onAdjust = onAdjustRest, onSkip = onSkipRest)
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        if (uiState.isLoading) return@Scaffold
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            items(items = uiState.exercises, key = { it.key }) { exercise ->
                val exerciseKey = exercise.key
                WorkoutExerciseCard(
                    exercise = exercise,
                    previousSets = exercise.exercise.id?.let { uiState.previousSets[it] }.orEmpty(),
                    weightUnit = uiState.weightUnit,
                    onRemove = { onRemoveExercise(exerciseKey) },
                    onAddSet = { onAddSet(exerciseKey) },
                    onDeleteSet = { setKey -> onDeleteSet(exerciseKey, setKey) },
                    onRepsChange = { setKey, reps -> onRepsChange(exerciseKey, setKey, reps) },
                    onWeightChange = { setKey, weight -> onWeightChange(exerciseKey, setKey, weight) },
                    onCompletedChange = { setKey, completed -> onSetCompletedChange(exerciseKey, setKey, completed) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                )
            }
            item(key = "workout-actions") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    OutlinedButton(onClick = onOpenAddExercises) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Text(text = stringResource(R.string.workout_add_exercises))
                    }
                    TextButton(
                        onClick = onDiscard,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(text = stringResource(R.string.workout_discard))
                    }
                }
            }
        }
    }

    if (uiState.isAddExercisesSheetOpen) {
        AddExercisesSheet(
            catalogExercises = uiState.catalogExercises,
            onDismiss = onDismissAddExercises,
            onConfirm = onConfirmAddExercises,
        )
    }
}

/** Time since [startedAt], ticking on whole seconds. */
@Composable
private fun ElapsedTime(startedAt: Instant) {
    val elapsedMs by produceState(initialValue = millisSince(startedAt), startedAt) {
        while (true) {
            value = millisSince(startedAt)
            delay(1_000 - value % 1_000)
        }
    }
    Text(
        text = formatDuration(elapsedMs / 1_000),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun millisSince(instant: Instant): Long = Duration.between(instant, Instant.now()).toMillis()

/** Remaining rest with a progress bar, ticking on whole seconds. */
@Composable
private fun RestTimerBar(
    timer: RestTimer,
    onAdjust: (deltaSeconds: Int) -> Unit,
    onSkip: () -> Unit,
) {
    val remainingMs by produceState(initialValue = timer.remainingMillis(System.currentTimeMillis()), timer) {
        while (value > 0) {
            delay((value % 1_000).takeIf { it > 0 } ?: 1_000)
            value = timer.remainingMillis(System.currentTimeMillis())
        }
    }
    val step = UserSettings.REST_STEP_SECONDS
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            LinearProgressIndicator(
                progress = { remainingMs / (timer.totalSeconds * 1_000f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            ) {
                Icon(painter = painterResource(R.drawable.ic_timer), contentDescription = null)
                Text(
                    // Rounded up, so it reads 0:01 during the last second, not 0:00.
                    text = formatDuration((remainingMs + 999) / 1_000),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                )
                TextButton(onClick = { onAdjust(-step) }) { Text(stringResource(R.string.rest_decrease, step)) }
                TextButton(onClick = { onAdjust(step) }) { Text(stringResource(R.string.rest_increase, step)) }
                TextButton(onClick = onSkip) { Text(stringResource(R.string.rest_skip)) }
            }
        }
    }
}

@Composable
private fun WorkoutDialogHost(
    dialog: WorkoutDialog?,
    routineName: String,
    onConfirmFinish: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onConfirmRemoveExercise: (exerciseKey: String) -> Unit,
    onResolveRoutineUpdate: (update: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        null -> Unit
        is WorkoutDialog.ConfirmFinish -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.workout_finish_title)) },
            text = if (dialog.uncheckedSets > 0) {
                {
                    Text(
                        pluralStringResource(
                            R.plurals.workout_unchecked_sets_dropped,
                            dialog.uncheckedSets,
                            dialog.uncheckedSets,
                        ),
                    )
                }
            } else {
                null
            },
            confirmButton = {
                TextButton(onClick = onConfirmFinish) { Text(stringResource(R.string.workout_finish)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            },
        )
        WorkoutDialog.NothingCompleted -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.workout_nothing_completed_title)) },
            text = { Text(stringResource(R.string.workout_nothing_completed_message)) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.workout_keep_going)) }
            },
            dismissButton = {
                TextButton(onClick = onConfirmDiscard) { Text(stringResource(R.string.workout_discard)) }
            },
        )
        WorkoutDialog.ConfirmDiscard -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.workout_discard_title)) },
            text = { Text(stringResource(R.string.workout_discard_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmDiscard) { Text(stringResource(R.string.action_discard)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            },
        )
        is WorkoutDialog.ConfirmRemoveExercise -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.workout_remove_exercise_title, dialog.name)) },
            text = { Text(stringResource(R.string.workout_remove_exercise_message)) },
            confirmButton = {
                TextButton(onClick = { onConfirmRemoveExercise(dialog.exerciseKey) }) {
                    Text(stringResource(R.string.action_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            },
        )
        WorkoutDialog.UpdateRoutine -> AlertDialog(
            onDismissRequest = { onResolveRoutineUpdate(false) },
            title = { Text(stringResource(R.string.workout_update_routine_title, routineName)) },
            text = { Text(stringResource(R.string.workout_update_routine_message)) },
            confirmButton = {
                TextButton(onClick = { onResolveRoutineUpdate(true) }) {
                    Text(stringResource(R.string.workout_update_routine))
                }
            },
            dismissButton = {
                TextButton(onClick = { onResolveRoutineUpdate(false) }) {
                    Text(stringResource(R.string.workout_keep_routine))
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutScreenPreview() {
    AppTheme {
        WorkoutScreen(
            uiState = WorkoutUiState(
                routineName = "Push Day",
                startedAt = Instant.now().minusSeconds(754),
                exercises = listOf(
                    WorkoutExerciseUiState(
                        exercise = Exercise(id = 1, name = "Bench Press", category = Category.CHEST),
                        sets = listOf(
                            WorkoutSetUiState(reps = 8, weightKg = 80.0, isCompleted = true),
                            WorkoutSetUiState(reps = 8, weightKg = 80.0),
                        ),
                    ),
                ),
                previousSets = mapOf(1 to listOf(LoggedSet(reps = 8, weightKg = 77.5, isCompleted = true))),
                restTimer = RestTimer(endsAtMillis = System.currentTimeMillis() + 62_000, totalSeconds = 90),
                isLoading = false,
            ),
            onBack = {},
            onFinish = {},
            onDiscard = {},
            onOpenAddExercises = {},
            onDismissAddExercises = {},
            onConfirmAddExercises = {},
            onRemoveExercise = {},
            onAddSet = {},
            onDeleteSet = { _, _ -> },
            onRepsChange = { _, _, _ -> },
            onWeightChange = { _, _, _ -> },
            onSetCompletedChange = { _, _, _ -> },
            onAdjustRest = {},
            onSkipRest = {},
            onConfirmFinish = {},
            onConfirmDiscard = {},
            onConfirmRemoveExercise = {},
            onResolveRoutineUpdate = {},
            onDismissDialog = {},
        )
    }
}
