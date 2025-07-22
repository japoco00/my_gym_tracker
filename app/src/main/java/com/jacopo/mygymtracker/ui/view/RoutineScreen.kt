package com.jacopo.mygymtracker.ui.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacopo.mygymtracker.model.Category
import com.jacopo.mygymtracker.ui.components.ExerciseCard
import com.jacopo.mygymtracker.viewModel.ExerciseUIState
import com.jacopo.mygymtracker.viewModel.RoutineUIState
import com.jacopo.mygymtracker.viewModel.RoutineViewVM
import com.jacopo.mygymtracker.viewModel.SetUIState

@Composable
fun RoutineScreen() {
    val vm: RoutineViewVM = viewModel()

    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(vm.staticData.exercises) { exercise ->
                ExerciseCard(exercise)
            }
        }
    }

}

@Preview(showBackground = true, apiLevel = 33)
@Composable
fun RoutineScreenPreview() {
    val sampleExercises = remember {
        mutableStateListOf(
            ExerciseUIState(
                name = mutableStateOf("Bench Press"),
                category = mutableStateOf(Category.CAT1),
                sets = mutableStateListOf(
                    SetUIState(mutableIntStateOf(10), mutableIntStateOf(60)),
                    SetUIState(mutableIntStateOf(8), mutableIntStateOf(70))
                )
            ), ExerciseUIState(
                name = mutableStateOf("Squat"),
                category = mutableStateOf(Category.CAT2),
                sets = mutableStateListOf(
                    SetUIState(mutableIntStateOf(12), mutableIntStateOf(80)),
                    SetUIState(mutableIntStateOf(10), mutableIntStateOf(90))
                )
            )
        )
    }

    val sampleUIState = RoutineUIState(
        routine = "Preview Routine", exercises = sampleExercises
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(sampleUIState.exercises) { exercise ->
                ExerciseCard(exercise)
            }
        }
    }

}
