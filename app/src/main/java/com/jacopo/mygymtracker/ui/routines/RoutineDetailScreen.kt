package com.jacopo.mygymtracker.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.SetType
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.domain.model.normalSetNumbers
import com.jacopo.mygymtracker.ui.components.AddExercisesButton
import com.jacopo.mygymtracker.ui.components.AddExercisesSheet
import com.jacopo.mygymtracker.ui.components.AppHeader
import com.jacopo.mygymtracker.ui.components.BackButton
import com.jacopo.mygymtracker.ui.components.ExerciseEditorSheet
import com.jacopo.mygymtracker.ui.components.Pill
import com.jacopo.mygymtracker.ui.components.RestEditorSheet
import com.jacopo.mygymtracker.ui.components.SetEditorSheet
import com.jacopo.mygymtracker.ui.components.headerTitle
import com.jacopo.mygymtracker.ui.theme.AppTheme

/** Creates or edits a routine. Edits save as you go, so there is no Save button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    uiState: RoutineDetailUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onOpenAddExercises: () -> Unit,
    onDismissAddExercises: () -> Unit,
    onConfirmAddExercises: (List<Exercise>) -> Unit,
    onOpenNewExercise: () -> Unit,
    onDismissNewExercise: () -> Unit,
    onConfirmNewExercise: (name: String, Category, TrackingType, isBarbell: Boolean) -> Unit,
    onRemoveExercise: (exerciseKey: String) -> Unit,
    onAddSet: (exerciseKey: String) -> Unit,
    onOpenRestEditor: (exerciseKey: String) -> Unit,
    onCloseRestEditor: () -> Unit,
    onRestChange: (exerciseKey: String, seconds: Int?) -> Unit,
    onOpenSetEditor: (exerciseKey: String, setKey: String) -> Unit,
    onCloseSetEditor: () -> Unit,
    onDeleteSet: (exerciseKey: String, setKey: String) -> Unit,
    onRepsChange: (exerciseKey: String, setKey: String, Int) -> Unit,
    onWeightChange: (exerciseKey: String, setKey: String, Double) -> Unit,
    onDurationChange: (exerciseKey: String, setKey: String, seconds: Int) -> Unit,
    onTypeChange: (exerciseKey: String, setKey: String, type: SetType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            if (!uiState.isLoading) {
                RoutineHeader(
                    name = uiState.name,
                    note = uiState.note.orEmpty(),
                    exerciseCount = uiState.exercises.size,
                    setCount = uiState.setCount,
                    scrollBehavior = scrollBehavior,
                    onBack = onBack,
                    onNameChange = onNameChange,
                    onNoteChange = onNoteChange,
                )
            }
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        if (uiState.isLoading) return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            items(items = uiState.exercises, key = { it.key }) { exercise ->
                val exerciseKey = exercise.key
                RoutineExerciseSection(
                    exercise = exercise,
                    weightUnit = uiState.weightUnit,
                    defaultRestSeconds = uiState.defaultRestSeconds,
                    onRemove = { onRemoveExercise(exerciseKey) },
                    onAddSet = { onAddSet(exerciseKey) },
                    onEditRest = { onOpenRestEditor(exerciseKey) },
                    onOpenSet = { setKey -> onOpenSetEditor(exerciseKey, setKey) },
                    onDeleteSet = { setKey -> onDeleteSet(exerciseKey, setKey) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                )
            }
            if (uiState.exercises.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.routine_editor_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
                    )
                }
            }
            item(key = "add-exercises") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    AddExercisesButton(onClick = onOpenAddExercises, text = stringResource(R.string.routine_add_from_catalog))
                    TextButton(onClick = onOpenNewExercise) {
                        Text(text = stringResource(R.string.routine_add_new_exercise))
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
        // No isCompleted: a planned set has nothing to tick, so the main button just closes.
        SetEditorSheet(
            exerciseName = exercise.name,
            number = exercise.sets.map { it.type }.normalSetNumbers()[index],
            trackingType = exercise.trackingType,
            weightUnit = uiState.weightUnit,
            reps = set.reps,
            weightKg = set.weightKg,
            durationSeconds = set.durationSeconds,
            type = set.type,
            plates = uiState.plates.takeIf { exercise.isBarbell },
            onRepsChange = { onRepsChange(exerciseKey, setKey, it) },
            onWeightChange = { onWeightChange(exerciseKey, setKey, it) },
            onDurationChange = { onDurationChange(exerciseKey, setKey, it) },
            onTypeChange = { onTypeChange(exerciseKey, setKey, it) },
            onDelete = { onDeleteSet(exerciseKey, setKey) },
            onDismiss = onCloseSetEditor,
        )
    }

    uiState.editingRestKey?.let { exerciseKey ->
        val exercise = uiState.exercises.find { it.key == exerciseKey } ?: return@let
        RestEditorSheet(
            exerciseName = exercise.name,
            restSeconds = exercise.restSeconds,
            defaultSeconds = uiState.defaultRestSeconds,
            onRestChange = { onRestChange(exerciseKey, it) },
            onDismiss = onCloseRestEditor,
        )
    }

    if (uiState.isAddExercisesSheetOpen) {
        AddExercisesSheet(
            catalogExercises = uiState.catalogExercises,
            onDismiss = onDismissAddExercises,
            onConfirm = onConfirmAddExercises,
        )
    }

    if (uiState.isNewExerciseSheetOpen) {
        ExerciseEditorSheet(
            exercise = null,
            onSave = onConfirmNewExercise,
            onDismiss = onDismissNewExercise,
            isNameTaken = uiState::isNameTaken,
        )
    }
}

/** The routine's name is the header's title, and editable in place, with its note underneath. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineHeader(
    name: String,
    note: String,
    exerciseCount: Int,
    setCount: Int,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
) {
    val untitled = stringResource(R.string.routine_untitled)
    val exercises = pluralStringResource(R.plurals.routine_exercise_count, exerciseCount, exerciseCount)
    AppHeader(
        title = { expanded ->
            if (expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HeaderTextField(
                        value = name,
                        onValueChange = onNameChange,
                        placeholder = untitled,
                        textStyle = MaterialTheme.typography.headerTitle.copy(color = LocalContentColor.current),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    HeaderTextField(
                        value = note,
                        onValueChange = onNoteChange,
                        placeholder = stringResource(R.string.routine_note_hint),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            } else {
                Text(text = name.ifBlank { untitled }, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            BackButton(onClick = onBack)
        },
        pills = {
            Pill { Text(text = exercises) }
            Pill { Text(text = pluralStringResource(R.plurals.history_set_count, setCount, setCount)) }
        },
        collapsedSubtitle = { Text(text = exercises) },
        // Default height plus the note line.
        expandedHeight = 196.dp,
    )
}

/** A borderless single-line field that looks like the text it edits, with a [placeholder] when empty. */
@Composable
private fun HeaderTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(text = placeholder, style = textStyle, color = MaterialTheme.colorScheme.outline)
                }
                innerTextField()
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true)
@Composable
private fun RoutineDetailScreenPreview() {
    AppTheme {
        RoutineDetailScreen(
            uiState = RoutineDetailUiState(
                id = 1,
                name = "Push day",
                isLoading = false,
                exercises = listOf(
                    RoutineExerciseUiState(
                        exerciseId = 1,
                        name = "Bench Press",
                        category = Category.CHEST,
                        sets = List(3) { PlannedSetUiState(reps = 8, weightKg = 80.0) },
                    ),
                    RoutineExerciseUiState(
                        exerciseId = 2,
                        name = "Plank",
                        category = Category.CORE,
                        trackingType = TrackingType.DURATION,
                        sets = List(2) { PlannedSetUiState(durationSeconds = 60) },
                    ),
                ),
            ),
            onBack = {},
            onNameChange = {},
            onNoteChange = {},
            onOpenAddExercises = {},
            onDismissAddExercises = {},
            onConfirmAddExercises = {},
            onOpenNewExercise = {},
            onDismissNewExercise = {},
            onConfirmNewExercise = { _, _, _, _ -> },
            onRemoveExercise = {},
            onAddSet = {},
            onOpenRestEditor = {},
            onCloseRestEditor = {},
            onRestChange = { _, _ -> },
            onOpenSetEditor = { _, _ -> },
            onCloseSetEditor = {},
            onDeleteSet = { _, _ -> },
            onRepsChange = { _, _, _ -> },
            onWeightChange = { _, _, _ -> },
            onDurationChange = { _, _, _ -> },
            onTypeChange = { _, _, _ -> },
        )
    }
}
