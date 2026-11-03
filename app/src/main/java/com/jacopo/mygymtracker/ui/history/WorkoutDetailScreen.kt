package com.jacopo.mygymtracker.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.WorkoutExercise
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import com.jacopo.mygymtracker.ui.components.AddExercisesSheet
import com.jacopo.mygymtracker.ui.components.AppHeader
import com.jacopo.mygymtracker.ui.components.DestructiveOverflowMenu
import com.jacopo.mygymtracker.ui.components.Pill
import com.jacopo.mygymtracker.ui.components.SetEditorSheet
import com.jacopo.mygymtracker.ui.components.ShapeBadge
import com.jacopo.mygymtracker.ui.format.formatDate
import com.jacopo.mygymtracker.ui.format.formatDuration
import com.jacopo.mygymtracker.ui.format.formatVolume
import com.jacopo.mygymtracker.ui.theme.AppTheme
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    uiState: WorkoutDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdits: () -> Unit,
    onDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onExerciseClick: (exerciseId: Int) -> Unit,
    onOpenAddExercises: () -> Unit,
    onDismissAddExercises: () -> Unit,
    onConfirmAddExercises: (List<Exercise>) -> Unit,
    onAddSet: (exerciseKey: String) -> Unit,
    onOpenSetEditor: (exerciseKey: String, setKey: String) -> Unit,
    onCloseSetEditor: () -> Unit,
    onDeleteSet: (exerciseKey: String, setKey: String) -> Unit,
    onRepsChange: (exerciseKey: String, setKey: String, Int) -> Unit,
    onWeightChange: (exerciseKey: String, setKey: String, Double) -> Unit,
    onDurationChange: (exerciseKey: String, setKey: String, seconds: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = uiState.isEditing, onBack = onCancelEdit)

    if (uiState.isDeleteDialogOpen) {
        AlertDialog(
            onDismissRequest = onDismissDeleteDialog,
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteDialog) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            uiState.workout?.let { workout ->
                WorkoutDetailHeader(
                    workout = workout,
                    uiState = uiState,
                    scrollBehavior = scrollBehavior,
                    onBack = onBack,
                    onEdit = onEdit,
                    onCancelEdit = onCancelEdit,
                    onSaveEdits = onSaveEdits,
                    onDelete = onDelete,
                )
            }
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        val workout = uiState.workout ?: return@Scaffold
        val editedExercises = uiState.editedExercises
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (editedExercises == null) {
                itemsIndexed(workout.exercises) { index, exercise ->
                    LoggedExerciseSection(
                        exercise = exercise,
                        prSetIndex = uiState.prSetIndexes.getOrNull(index),
                        weightUnit = uiState.weightUnit,
                        onClick = exercise.exercise.id?.let { exerciseId -> { onExerciseClick(exerciseId) } },
                    )
                }
            } else {
                if (!uiState.canSave) {
                    item(key = "needs-set") {
                        Text(
                            text = stringResource(R.string.history_edit_needs_set),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp),
                        )
                    }
                }
                items(items = editedExercises, key = { it.key }) { exercise ->
                    val exerciseKey = exercise.key
                    EditableExerciseSection(
                        exercise = exercise,
                        weightUnit = uiState.weightUnit,
                        onAddSet = { onAddSet(exerciseKey) },
                        onOpenSet = { setKey -> onOpenSetEditor(exerciseKey, setKey) },
                        onDeleteSet = { setKey -> onDeleteSet(exerciseKey, setKey) },
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
    }

    uiState.editingSet?.let { editing ->
        val exercise = uiState.editedExercises?.find { it.key == editing.exerciseKey } ?: return@let
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

/**
 * The routine's name over date, duration and volume pills, with a sets badge. Viewing offers
 * Edit and Delete; editing swaps them for Close and Save.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WorkoutDetailHeader(
    workout: WorkoutSession,
    uiState: WorkoutDetailUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdits: () -> Unit,
    onDelete: () -> Unit,
) {
    val date = formatDate(workout.startedAt)
    AppHeader(
        title = workout.routineName,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            if (uiState.isEditing) {
                IconButton(onClick = onCancelEdit) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                }
            } else {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            }
        },
        actions = {
            ShapeBadge(
                shape = MaterialShapes.SoftBurst,
                value = uiState.setCount.toString(),
                label = stringResource(R.string.workout_sets_badge),
                contentDescription = pluralStringResource(R.plurals.history_set_count, uiState.setCount, uiState.setCount),
            )
            if (uiState.isEditing) {
                Button(
                    onClick = onSaveEdits,
                    enabled = uiState.canSave,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(R.string.action_save))
                }
            } else {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                }
                DestructiveOverflowMenu(action = stringResource(R.string.history_delete), onAction = onDelete)
            }
        },
        pills = {
            Pill {
                Icon(painter = painterResource(R.drawable.ic_history), contentDescription = null, modifier = Modifier.size(18.dp))
                Text(text = date)
            }
            Pill {
                Icon(painter = painterResource(R.drawable.ic_timer), contentDescription = null, modifier = Modifier.size(18.dp))
                Text(text = formatDuration(uiState.durationSeconds))
            }
            if (uiState.volumeKg > 0) {
                Pill { Text(text = uiState.weightUnit.formatVolume(uiState.volumeKg)) }
            }
        },
        collapsedSubtitle = {
            Text(text = if (uiState.isEditing) stringResource(R.string.history_edit_title) else date)
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun WorkoutDetailScreenPreview() {
    val now = Instant.now()
    AppTheme {
        WorkoutDetailScreen(
            uiState = WorkoutDetailUiState(
                workout = WorkoutSession(
                    id = 1,
                    routineId = 1,
                    routineName = "Push Day",
                    startedAt = now.minusSeconds(3_134),
                    finishedAt = now,
                    exercises = listOf(
                        WorkoutExercise(
                            exercise = Exercise(id = 1, name = "Bench Press", category = Category.CHEST),
                            sets = listOf(
                                LoggedSet(reps = 8, weightKg = 80.0, isCompleted = true),
                                LoggedSet(reps = 7, weightKg = 82.5, isCompleted = true),
                            ),
                        ),
                    ),
                ),
                prSetIndexes = listOf(1),
            ),
            onBack = {},
            onEdit = {},
            onCancelEdit = {},
            onSaveEdits = {},
            onDelete = {},
            onConfirmDelete = {},
            onDismissDeleteDialog = {},
            onExerciseClick = {},
            onOpenAddExercises = {},
            onDismissAddExercises = {},
            onConfirmAddExercises = {},
            onAddSet = {},
            onOpenSetEditor = { _, _ -> },
            onCloseSetEditor = {},
            onDeleteSet = { _, _ -> },
            onRepsChange = { _, _, _ -> },
            onWeightChange = { _, _, _ -> },
            onDurationChange = { _, _, _ -> },
        )
    }
}
