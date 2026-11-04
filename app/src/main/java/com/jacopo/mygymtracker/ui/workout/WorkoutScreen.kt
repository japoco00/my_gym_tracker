package com.jacopo.mygymtracker.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.domain.model.UserSettings
import com.jacopo.mygymtracker.resttimer.RestTimer
import com.jacopo.mygymtracker.ui.components.AddExercisesSheet
import com.jacopo.mygymtracker.ui.components.SetEditorSheet
import com.jacopo.mygymtracker.ui.format.formatDuration
import com.jacopo.mygymtracker.ui.theme.AppTheme
import kotlinx.coroutines.delay
import java.time.Instant

/** Keeps the last row scrollable above the floating Finish button / rest toolbar. */
private val FloatingControlsClearance = 96.dp

/** The rest number pulses on each of these final seconds. */
private const val FINAL_COUNTDOWN_SECONDS = 3L

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
    onOpenSetEditor: (exerciseKey: String, setKey: String) -> Unit,
    onCloseSetEditor: () -> Unit,
    onDeleteSet: (exerciseKey: String, setKey: String) -> Unit,
    onRepsChange: (exerciseKey: String, setKey: String, Int) -> Unit,
    onWeightChange: (exerciseKey: String, setKey: String, Double) -> Unit,
    onDurationChange: (exerciseKey: String, setKey: String, seconds: Int) -> Unit,
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            if (!uiState.isLoading) {
                WorkoutHeader(
                    routineName = uiState.routineName,
                    startedAt = uiState.startedAt,
                    completedSets = uiState.completedSets,
                    totalSets = uiState.totalSets,
                    completedVolumeKg = uiState.completedVolumeKg,
                    weightUnit = uiState.weightUnit,
                    scrollBehavior = scrollBehavior,
                    onBack = onBack,
                    onDiscard = onDiscard,
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isLoading) {
                FloatingControls(
                    restTimer = uiState.restTimer,
                    onAdjustRest = onAdjustRest,
                    onSkipRest = onSkipRest,
                    onFinish = onFinish,
                )
            }
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        if (uiState.isLoading) return@Scaffold
        LazyColumn(
            contentPadding = PaddingValues(bottom = FloatingControlsClearance),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            items(items = uiState.exercises, key = { it.key }) { exercise ->
                val exerciseKey = exercise.key
                WorkoutExerciseSection(
                    exercise = exercise,
                    previousSets = exercise.exercise.id?.let { uiState.previousSets[it] }.orEmpty(),
                    weightUnit = uiState.weightUnit,
                    onRemove = { onRemoveExercise(exerciseKey) },
                    onAddSet = { onAddSet(exerciseKey) },
                    onOpenSet = { setKey -> onOpenSetEditor(exerciseKey, setKey) },
                    onDeleteSet = { setKey -> onDeleteSet(exerciseKey, setKey) },
                    onCompletedChange = { setKey, completed -> onSetCompletedChange(exerciseKey, setKey, completed) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                )
            }
            item(key = "add-exercises") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    FilledTonalButton(onClick = onOpenAddExercises, shapes = ButtonDefaults.shapes()) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text(text = stringResource(R.string.workout_add_exercises))
                    }
                }
            }
        }
    }

    uiState.editingSet?.let { editing ->
        val exercise = uiState.exercises.find { it.key == editing.exerciseKey } ?: return@let
        val index = exercise.sets.indexOfFirst { it.key == editing.setKey }.takeIf { it >= 0 } ?: return@let
        val exerciseKey = exercise.key
        val setKey = editing.setKey
        val set = exercise.sets[index]
        SetEditorSheet(
            exerciseName = exercise.exercise.name,
            number = index + 1,
            trackingType = exercise.exercise.trackingType,
            weightUnit = uiState.weightUnit,
            reps = set.reps,
            weightKg = set.weightKg,
            durationSeconds = set.durationSeconds,
            onRepsChange = { onRepsChange(exerciseKey, setKey, it) },
            onWeightChange = { onWeightChange(exerciseKey, setKey, it) },
            onDurationChange = { onDurationChange(exerciseKey, setKey, it) },
            onDelete = { onDeleteSet(exerciseKey, setKey) },
            onDismiss = onCloseSetEditor,
            previous = exercise.exercise.id?.let { uiState.previousSets[it]?.getOrNull(index) },
            isCompleted = set.isCompleted,
            onCompletedChange = { onSetCompletedChange(exerciseKey, setKey, it) },
        )
    }

    if (uiState.isAddExercisesSheetOpen) {
        AddExercisesSheet(
            catalogExercises = uiState.catalogExercises,
            onDismiss = onDismissAddExercises,
            onConfirm = onConfirmAddExercises,
        )
    }
}

/** The Finish FAB. While resting it shrinks to its icon and the rest toolbar unfolds out of its left side. */
@Composable
private fun FloatingControls(
    restTimer: RestTimer?,
    onAdjustRest: (deltaSeconds: Int) -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
) {
    val motionScheme = MaterialTheme.motionScheme
    // Keeps the last timer on screen while the toolbar folds away.
    var lastTimer by remember { mutableStateOf(restTimer) }
    if (restTimer != null) lastTimer = restTimer
    Row(verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(
            visible = restTimer != null,
            enter = expandHorizontally(motionScheme.defaultSpatialSpec(), expandFrom = Alignment.End) +
                fadeIn(motionScheme.defaultEffectsSpec()),
            exit = shrinkHorizontally(motionScheme.fastSpatialSpec(), shrinkTowards = Alignment.End) +
                fadeOut(motionScheme.fastEffectsSpec()),
        ) {
            lastTimer?.let {
                RestToolbar(
                    timer = it,
                    onAdjust = onAdjustRest,
                    onSkip = onSkipRest,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        val finish = stringResource(R.string.workout_finish)
        ExtendedFloatingActionButton(
            text = { Text(text = finish) },
            // The label folds away while resting, so the icon has to speak for itself.
            icon = { Icon(imageVector = Icons.Default.Check, contentDescription = finish.takeIf { restTimer != null }) },
            onClick = onFinish,
            expanded = restTimer == null,
        )
    }
}

/**
 * Remaining rest in a wavy ring, with +15 s and skip. Ticks on whole seconds; the ring glides between
 * ticks and the number pulses through the last three.
 */
@Composable
private fun RestToolbar(
    timer: RestTimer,
    onAdjust: (deltaSeconds: Int) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val remainingMs by produceState(initialValue = timer.remainingMillis(System.currentTimeMillis()), timer) {
        while (value > 0) {
            delay((value % 1_000).takeIf { it > 0 } ?: 1_000)
            value = timer.remainingMillis(System.currentTimeMillis())
        }
    }
    val progress by animateFloatAsState(
        targetValue = remainingMs / (timer.totalSeconds * 1_000f),
        animationSpec = tween(durationMillis = 1_000, easing = LinearEasing),
    )
    // Rounded up, so it reads 0:01 during the last second, not 0:00.
    val remainingSeconds = (remainingMs + 999) / 1_000
    val pulse = remember { Animatable(1f) }
    val pulseSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds in 1..FINAL_COUNTDOWN_SECONDS) {
            pulse.snapTo(1.15f)
            pulse.animateTo(1f, pulseSpec)
        }
    }
    val step = UserSettings.REST_STEP_SECONDS
    HorizontalFloatingToolbar(
        expanded = true,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
            CircularWavyProgressIndicator(
                progress = { progress },
                trackColor = LocalContentColor.current.copy(alpha = 0.2f),
                modifier = Modifier.size(44.dp),
            )
            Icon(
                painter = painterResource(R.drawable.ic_timer),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = formatDuration(remainingSeconds),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .graphicsLayer {
                    scaleX = pulse.value
                    scaleY = pulse.value
                },
        )
        TextButton(
            onClick = { onAdjust(step) },
            colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
        ) {
            Text(text = stringResource(R.string.rest_increase, step))
        }
        IconButton(onClick = onSkip) {
            Icon(
                painter = painterResource(R.drawable.ic_skip_next),
                contentDescription = stringResource(R.string.rest_skip_description),
            )
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
    WorkoutScreenSample(restTimer = null)
}

@Preview(showBackground = true)
@Composable
private fun WorkoutScreenRestingPreview() {
    WorkoutScreenSample(
        restTimer = RestTimer(endsAtMillis = System.currentTimeMillis() + 62_000, totalSeconds = 90),
    )
}

@Composable
private fun WorkoutScreenSample(restTimer: RestTimer?) {
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
                    WorkoutExerciseUiState(
                        exercise = Exercise(
                            id = 2,
                            name = "Plank",
                            category = Category.CORE,
                            trackingType = TrackingType.DURATION,
                        ),
                        sets = listOf(WorkoutSetUiState(durationSeconds = 60)),
                    ),
                ),
                previousSets = mapOf(1 to listOf(LoggedSet(reps = 8, weightKg = 77.5, isCompleted = true))),
                restTimer = restTimer,
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
            onOpenSetEditor = { _, _ -> },
            onCloseSetEditor = {},
            onDeleteSet = { _, _ -> },
            onRepsChange = { _, _, _ -> },
            onWeightChange = { _, _, _ -> },
            onDurationChange = { _, _, _ -> },
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
