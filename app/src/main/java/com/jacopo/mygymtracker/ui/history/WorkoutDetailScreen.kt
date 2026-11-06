package com.jacopo.mygymtracker.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import com.jacopo.mygymtracker.domain.model.SetType
import com.jacopo.mygymtracker.domain.model.WorkoutExercise
import com.jacopo.mygymtracker.domain.model.WorkoutSession
import com.jacopo.mygymtracker.domain.model.normalSetNumbers
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
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Lets the screen slide mostly in before the celebration starts. */
private const val CELEBRATION_DELAY_MS = 300L
private const val COUNT_UP_MS = 900
private const val PILL_DELAY_MS = 150L
private const val PILL_STAGGER_MS = 70L
private const val PILL_RISE_MS = 300

/** Material's emphasized decelerate curve: fast out of the gate, long settle. */
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

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
    onTypeChange: (exerciseKey: String, setKey: String, type: SetType) -> Unit,
    onRpeChange: (exerciseKey: String, setKey: String, rpe: Double?) -> Unit,
    modifier: Modifier = Modifier,
    celebrate: Boolean = false,
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
                    celebrate = celebrate,
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
            number = exercise.sets.map { it.type }.normalSetNumbers()[index],
            trackingType = exercise.exercise.trackingType,
            weightUnit = uiState.weightUnit,
            reps = set.reps,
            weightKg = set.weightKg,
            durationSeconds = set.durationSeconds,
            type = set.type,
            onRepsChange = { onRepsChange(exerciseKey, setKey, it) },
            onWeightChange = { onWeightChange(exerciseKey, setKey, it) },
            onDurationChange = { onDurationChange(exerciseKey, setKey, it) },
            onTypeChange = { onTypeChange(exerciseKey, setKey, it) },
            onDelete = { onDeleteSet(exerciseKey, setKey) },
            onDismiss = onCloseSetEditor,
            plates = uiState.plates.takeIf { exercise.exercise.isBarbell },
            rpe = set.rpe,
            onRpeChange = { onRpeChange(exerciseKey, setKey, it) },
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
 * Edit and Delete; editing swaps them for Close and Save. With [celebrate], plays [Celebration].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WorkoutDetailHeader(
    workout: WorkoutSession,
    uiState: WorkoutDetailUiState,
    celebrate: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdits: () -> Unit,
    onDelete: () -> Unit,
) {
    val date = formatDate(workout.startedAt)
    val celebration = rememberCelebration(celebrate)
    val countUp = celebration.countUp.value
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
                value = (uiState.setCount * countUp).roundToInt().toString(),
                label = stringResource(R.string.workout_sets_badge),
                contentDescription = pluralStringResource(R.plurals.history_set_count, uiState.setCount, uiState.setCount),
                modifier = Modifier.graphicsLayer {
                    scaleX = celebration.badgeScale.value
                    scaleY = celebration.badgeScale.value
                },
                shapeModifier = Modifier.graphicsLayer { rotationZ = celebration.shapeRotation.value },
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
            Pill(modifier = Modifier.risingIn(celebration.pills[0])) {
                Icon(painter = painterResource(R.drawable.ic_history), contentDescription = null, modifier = Modifier.size(18.dp))
                Text(text = date)
            }
            Pill(modifier = Modifier.risingIn(celebration.pills[1])) {
                Icon(painter = painterResource(R.drawable.ic_timer), contentDescription = null, modifier = Modifier.size(18.dp))
                Text(text = formatDuration((uiState.durationSeconds * countUp).toLong()))
            }
            if (uiState.volumeKg > 0) {
                Pill(modifier = Modifier.risingIn(celebration.pills[2])) {
                    Text(text = uiState.weightUnit.formatVolume(uiState.volumeKg * countUp))
                }
            }
        },
        collapsedSubtitle = {
            Text(text = if (uiState.isEditing) stringResource(R.string.history_edit_title) else date)
        },
    )
}

/**
 * The finish celebration: the sets badge pops in spinning, the pills rise in one by one and the
 * numbers count up from zero. Everything starts at its end when there's nothing to play.
 */
@Stable
private class Celebration(played: Boolean) {
    val badgeScale = Animatable(if (played) 1f else 0f)
    val shapeRotation = Animatable(if (played) 0f else -120f)
    val countUp = Animatable(if (played) 1f else 0f)
    val pills = List(3) { Animatable(if (played) 1f else 0f) }

    suspend fun play() = coroutineScope {
        launch { badgeScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy)) }
        launch { shapeRotation.animateTo(0f, spring(stiffness = Spring.StiffnessLow)) }
        launch { countUp.animateTo(1f, tween(COUNT_UP_MS, easing = EmphasizedDecelerate)) }
        pills.forEachIndexed { index, pill ->
            launch {
                delay(PILL_DELAY_MS + PILL_STAGGER_MS * index)
                pill.animateTo(1f, tween(PILL_RISE_MS, easing = LinearOutSlowInEasing))
            }
        }
    }
}

/**
 * Plays the [Celebration] once, when [celebrate] and the screen is mostly in. Saved, so rotating
 * or coming back from another screen doesn't replay it.
 */
@Composable
private fun rememberCelebration(celebrate: Boolean): Celebration {
    var played by rememberSaveable { mutableStateOf(!celebrate) }
    val celebration = remember { Celebration(played) }
    LaunchedEffect(Unit) {
        if (played) return@LaunchedEffect
        played = true
        delay(CELEBRATION_DELAY_MS)
        celebration.play()
    }
    return celebration
}

/** Fades in while rising its last 8 dp, as [progress] goes from 0 to 1. */
private fun Modifier.risingIn(progress: Animatable<Float, *>): Modifier = graphicsLayer {
    alpha = progress.value
    translationY = (1f - progress.value) * 8.dp.toPx()
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
            onTypeChange = { _, _, _ -> },
            onRpeChange = { _, _, _ -> },
        )
    }
}
