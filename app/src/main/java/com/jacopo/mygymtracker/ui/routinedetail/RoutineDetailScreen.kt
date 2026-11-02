package com.jacopo.mygymtracker.ui.routinedetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.jacopo.mygymtracker.ui.components.SwipeToDeleteContainer
import com.jacopo.mygymtracker.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    uiState: RoutineDetailUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onExerciseNameChange: (exerciseKey: String, String) -> Unit,
    onAddEmptyExercise: () -> Unit,
    onOpenCatalogSheet: () -> Unit,
    onDismissCatalogSheet: () -> Unit,
    onConfirmCatalogExercises: (List<Exercise>) -> Unit,
    onDeleteExercise: (exerciseKey: String) -> Unit,
    onAddSet: (exerciseKey: String) -> Unit,
    onDeleteSet: (exerciseKey: String, setUuid: String) -> Unit,
    onRepsChange: (exerciseKey: String, setUuid: String, Int) -> Unit,
    onWeightChange: (exerciseKey: String, setUuid: String, Double) -> Unit,
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TextField(
                value = uiState.name,
                onValueChange = onNameChange,
                placeholder = { Text(text = "Routine name") },
                singleLine = true,
                isError = uiState.name.isBlank(),
                supportingText = {
                    if (uiState.name.isBlank()) {
                        Text(text = "Routine name cannot be empty")
                    }
                },
                trailingIcon = {
                    if (uiState.name.isNotEmpty()) {
                        IconButton(onClick = { onNameChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear routine name",
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
                        ExerciseCard(
                            exercise = exercise,
                            onNameChange = { name -> onExerciseNameChange(exerciseKey, name) },
                            onAddSet = { onAddSet(exerciseKey) },
                            onDeleteSet = { setUuid -> onDeleteSet(exerciseKey, setUuid) },
                            onRepsChange = { setUuid, reps -> onRepsChange(exerciseKey, setUuid, reps) },
                            onWeightChange = { setUuid, weight ->
                                onWeightChange(exerciseKey, setUuid, weight)
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
                OutlinedButton(onClick = onOpenCatalogSheet) {
                    Text(text = "From Catalog")
                }
                Button(onClick = onAddEmptyExercise) {
                    Text(text = "New Exercise")
                }
            }
        }
    }

    if (uiState.isAddExercisesSheetOpen) {
        AddExercisesModal(
            catalogExercises = uiState.catalogExercises,
            onDismiss = onDismissCatalogSheet,
            onConfirm = onConfirmCatalogExercises,
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
                    ExerciseUiState(
                        exerciseId = 1,
                        name = "Bench Press",
                        category = Category.CHEST,
                        sets = listOf(SetUiState(reps = 10, weightKg = 60.0)),
                    ),
                    ExerciseUiState(
                        exerciseId = 2,
                        name = "Squat",
                        category = Category.LEGS,
                        sets = listOf(SetUiState(reps = 8, weightKg = 100.0)),
                    ),
                ),
            ),
            onBack = {},
            onNameChange = {},
            onExerciseNameChange = { _, _ -> },
            onAddEmptyExercise = {},
            onOpenCatalogSheet = {},
            onDismissCatalogSheet = {},
            onConfirmCatalogExercises = {},
            onDeleteExercise = {},
            onAddSet = {},
            onDeleteSet = { _, _ -> },
            onRepsChange = { _, _, _ -> },
            onWeightChange = { _, _, _ -> },
        )
    }
}
