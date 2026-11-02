package com.jacopo.mygymtracker.ui.routines

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.domain.model.Category
import com.jacopo.mygymtracker.domain.model.Exercise
import com.jacopo.mygymtracker.domain.model.TrackingType
import com.jacopo.mygymtracker.ui.components.AddExercisesSheet
import com.jacopo.mygymtracker.ui.components.SwipeToDeleteContainer
import com.jacopo.mygymtracker.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    uiState: RoutineDetailUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onExerciseNameChange: (exerciseKey: String, String) -> Unit,
    onExerciseCategoryChange: (exerciseKey: String, Category) -> Unit,
    onExerciseTrackingTypeChange: (exerciseKey: String, TrackingType) -> Unit,
    onAddEmptyExercise: () -> Unit,
    onOpenAddExercises: () -> Unit,
    onDismissAddExercises: () -> Unit,
    onConfirmAddExercises: (List<Exercise>) -> Unit,
    onDeleteExercise: (exerciseKey: String) -> Unit,
    onAddSet: (exerciseKey: String) -> Unit,
    onDeleteSet: (exerciseKey: String, setKey: String) -> Unit,
    onRepsChange: (exerciseKey: String, setKey: String, Int) -> Unit,
    onWeightChange: (exerciseKey: String, setKey: String, Double) -> Unit,
    onDurationChange: (exerciseKey: String, setKey: String, seconds: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.id == null) R.string.routine_editor_title_new
                            else R.string.routine_editor_title_edit,
                        ),
                    )
                },
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
        if (uiState.isLoading) return@Scaffold
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            TextField(
                value = uiState.name,
                onValueChange = onNameChange,
                placeholder = { Text(text = stringResource(R.string.routine_name_hint)) },
                singleLine = true,
                trailingIcon = {
                    if (uiState.name.isNotEmpty()) {
                        IconButton(onClick = { onNameChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.action_clear),
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                items(
                    items = uiState.exercises,
                    key = { it.key },
                ) { exercise ->
                    val exerciseKey = exercise.key
                    SwipeToDeleteContainer(
                        onDelete = { onDeleteExercise(exerciseKey) },
                        modifier = Modifier.animateItem(),
                    ) {
                        RoutineExerciseCard(
                            exercise = exercise,
                            weightUnit = uiState.weightUnit,
                            onNameChange = { name -> onExerciseNameChange(exerciseKey, name) },
                            onCategoryChange = { category ->
                                onExerciseCategoryChange(exerciseKey, category)
                            },
                            onTrackingTypeChange = { trackingType ->
                                onExerciseTrackingTypeChange(exerciseKey, trackingType)
                            },
                            onAddSet = { onAddSet(exerciseKey) },
                            onDeleteSet = { setKey -> onDeleteSet(exerciseKey, setKey) },
                            onRepsChange = { setKey, reps -> onRepsChange(exerciseKey, setKey, reps) },
                            onWeightChange = { setKey, weight ->
                                onWeightChange(exerciseKey, setKey, weight)
                            },
                            onDurationChange = { setKey, seconds ->
                                onDurationChange(exerciseKey, setKey, seconds)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                OutlinedButton(onClick = onOpenAddExercises) {
                    Text(text = stringResource(R.string.routine_add_from_catalog))
                }
                Button(onClick = onAddEmptyExercise) {
                    Text(text = stringResource(R.string.routine_add_new_exercise))
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

@Preview(showBackground = true)
@Composable
private fun RoutineDetailScreenPreview() {
    AppTheme {
        RoutineDetailScreen(
            uiState = RoutineDetailUiState(
                id = 1,
                name = "Sample Routine",
                isLoading = false,
                exercises = listOf(
                    RoutineExerciseUiState(
                        exerciseId = 1,
                        name = "Bench Press",
                        category = Category.CHEST,
                        sets = listOf(PlannedSetUiState(reps = 10, weightKg = 60.0)),
                    ),
                    RoutineExerciseUiState(
                        exerciseId = 2,
                        name = "Squat",
                        category = Category.LEGS,
                        sets = listOf(PlannedSetUiState(reps = 8, weightKg = 100.0)),
                    ),
                ),
            ),
            onBack = {},
            onNameChange = {},
            onExerciseNameChange = { _, _ -> },
            onExerciseCategoryChange = { _, _ -> },
            onExerciseTrackingTypeChange = { _, _ -> },
            onAddEmptyExercise = {},
            onOpenAddExercises = {},
            onDismissAddExercises = {},
            onConfirmAddExercises = {},
            onDeleteExercise = {},
            onAddSet = {},
            onDeleteSet = { _, _ -> },
            onRepsChange = { _, _, _ -> },
            onWeightChange = { _, _, _ -> },
            onDurationChange = { _, _, _ -> },
        )
    }
}
