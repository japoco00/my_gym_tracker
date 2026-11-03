package com.jacopo.mygymtracker.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.LoggedSet
import com.jacopo.mygymtracker.domain.model.WeightUnit
import com.jacopo.mygymtracker.domain.model.WorkoutExercise
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import com.jacopo.mygymtracker.ui.format.formatDateTime
import com.jacopo.mygymtracker.ui.format.formatDuration
import com.jacopo.mygymtracker.ui.format.formatSet
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
    onAddSet: (exerciseKey: String) -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditing) {
                            stringResource(R.string.history_edit_title)
                        } else {
                            uiState.workout?.routineName.orEmpty()
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
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
                    if (uiState.isEditing) {
                        TextButton(onClick = onSaveEdits, enabled = uiState.canSave) {
                            Text(stringResource(R.string.action_save))
                        }
                    } else {
                        IconButton(onClick = onEdit, enabled = uiState.workout != null) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = onDelete, enabled = uiState.workout != null) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        val workout = uiState.workout ?: return@Scaffold
        val editedExercises = uiState.editedExercises
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                WorkoutStats(
                    startedAt = workout.startedAt,
                    durationSeconds = uiState.durationSeconds,
                    setCount = uiState.setCount,
                    volume = uiState.weightUnit.formatVolume(uiState.volumeKg),
                )
            }
            if (editedExercises == null) {
                items(workout.exercises) { exercise ->
                    ExerciseSetsCard(
                        exercise = exercise,
                        weightUnit = uiState.weightUnit,
                        onClick = { onExerciseClick(checkNotNull(exercise.exercise.id)) },
                    )
                }
            } else {
                if (!uiState.canSave) {
                    item {
                        Text(
                            text = stringResource(R.string.history_edit_needs_set),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                items(editedExercises, key = { it.key }) { exercise ->
                    EditableExerciseCard(
                        exercise = exercise,
                        weightUnit = uiState.weightUnit,
                        onAddSet = { onAddSet(exercise.key) },
                        onDeleteSet = { setKey -> onDeleteSet(exercise.key, setKey) },
                        onRepsChange = { setKey, reps -> onRepsChange(exercise.key, setKey, reps) },
                        onWeightChange = { setKey, weight -> onWeightChange(exercise.key, setKey, weight) },
                        onDurationChange = { setKey, seconds -> onDurationChange(exercise.key, setKey, seconds) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutStats(
    startedAt: Instant,
    durationSeconds: Long,
    setCount: Int,
    volume: String,
) {
    Column {
        Text(
            text = formatDateTime(startedAt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.padding(top = 12.dp)) {
            Stat(stringResource(R.string.history_duration), formatDuration(durationSeconds), Modifier.weight(1f))
            Stat(stringResource(R.string.history_sets), setCount.toString(), Modifier.weight(1f))
            Stat(stringResource(R.string.history_volume), volume, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ExerciseSetsCard(
    exercise: WorkoutExercise,
    weightUnit: WeightUnit,
    onClick: () -> Unit,
) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = exercise.exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            exercise.sets.forEachIndexed { index, set ->
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = (index + 1).toString(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(32.dp),
                    )
                    Text(
                        formatSet(
                            trackingType = exercise.exercise.trackingType,
                            weightUnit = weightUnit,
                            reps = set.reps,
                            weightKg = set.weightKg,
                            durationSeconds = set.durationSeconds,
                        ),
                    )
                }
            }
        }
    }
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
                                LoggedSet(reps = 7, weightKg = 80.0, isCompleted = true),
                            ),
                        ),
                    ),
                ),
            ),
            onBack = {},
            onEdit = {},
            onCancelEdit = {},
            onSaveEdits = {},
            onDelete = {},
            onConfirmDelete = {},
            onDismissDeleteDialog = {},
            onExerciseClick = {},
            onAddSet = {},
            onDeleteSet = { _, _ -> },
            onRepsChange = { _, _, _ -> },
            onWeightChange = { _, _, _ -> },
            onDurationChange = { _, _, _ -> },
        )
    }
}
