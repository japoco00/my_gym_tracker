package com.jacopo.mygymtracker.ui.screen

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jacopo.mygymtracker.model.Category
import com.jacopo.mygymtracker.ui.AddExercisesUIState
import com.jacopo.mygymtracker.ui.ExerciseUIState
import com.jacopo.mygymtracker.ui.RoutineUIState
import com.jacopo.mygymtracker.ui.components.AddExercisesModal
import com.jacopo.mygymtracker.ui.components.ExerciseCard
import com.jacopo.mygymtracker.ui.components.SwipableElement
import com.jacopo.mygymtracker.ui.toUiState

@Composable
fun RoutineScreen(
    uiState: State<RoutineUIState?>,
    onBack: () -> Unit = {},
    onAddEmptyExercise: () -> Unit = {},
    onDeleteExercise: (ExerciseUIState) -> Unit = {},
    onAddNewSet: (ExerciseUIState) -> Unit = {},
    onAddExercises: (List<ExerciseUIState>) -> Unit = {}
) {
    var showModal by remember { mutableStateOf(false) }

    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = uiState.value?.name?.value ?: "",
            onValueChange = { uiState.value?.name?.value = it },
            placeholder = { Text("Routine Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true,
            trailingIcon = {
                IconButton(
                    onClick = { uiState.value?.name?.value = "" }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Routine Name",
                    )
                }
            },
            isError = uiState.value?.name?.value.isNullOrEmpty(),
            supportingText = {
                if (uiState.value?.name?.value.isNullOrEmpty())
                    Text("Routine name cannot be empty")
            },
        )
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            items(
                uiState.value?.exercises ?: emptyList(),
                key = { it.id ?: it.draftId }
            ) { exercise ->
                SwipableElement(
                    item = exercise,
                    onDismiss = { onDeleteExercise(exercise) },
                    content = {
                        ExerciseCard(
                            exercise = exercise,
                            addNewSet = { onAddNewSet(exercise) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }
        Button(
            onClick = { onAddEmptyExercise() },
            modifier = Modifier.padding(0.dp, 16.dp)
        ) {
            Text("Add Exercise")
        }
    }

    AddExercisesModal (
        uiState = AddExercisesUIState()
    ) { exerciseList ->
        val exerciseUiStateList = exerciseList.map { exercise ->
            exercise.toUiState()
        }
        onAddExercises(exerciseUiStateList)
    }

}

@SuppressLint("UnrememberedMutableState")
@Preview
@Composable
fun RoutineScreenPreview() {
    RoutineScreen(
        uiState = mutableStateOf(
            RoutineUIState(
                id = 1,
                name = mutableStateOf("Sample Routine"),
                exercises = mutableStateListOf(
                    ExerciseUIState(
                        id = 1,
                        name = mutableStateOf("Bench Press"),
                        sets = mutableStateOf(listOf()),
                        draftId = "1",
                        category = mutableStateOf(Category.CAT1)
                    ),
                    ExerciseUIState(
                        id = 2,
                        name = mutableStateOf("Squat"),
                        sets = mutableStateOf(listOf()),
                        draftId = "2",
                        category = mutableStateOf(Category.CAT1)
                    )
                )
            )
        ),
        onBack = {},
        onAddEmptyExercise = {},
        onDeleteExercise = {},
        onAddNewSet = {}
    )
}

