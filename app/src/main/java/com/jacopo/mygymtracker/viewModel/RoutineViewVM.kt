package com.jacopo.mygymtracker.viewModel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.jacopo.mygymtracker.model.Category

class RoutineViewVM : ViewModel() {
    val staticData = RoutineViewUIState(
        routineName = "My Routine",
        exercises = mutableStateListOf(
            ExerciseUIState(
                name = mutableStateOf("Bench Press"),
                category = mutableStateOf(Category.CAT1),
                sets = mutableStateListOf(
                    SetUIState(mutableIntStateOf(10), mutableIntStateOf(60)),
                    SetUIState(mutableIntStateOf(8), mutableIntStateOf(70))
                )
            ),
            ExerciseUIState(
                name = mutableStateOf("Squat"),
                category = mutableStateOf(Category.CAT2),
                sets = mutableStateListOf(
                    SetUIState(mutableIntStateOf(12), mutableIntStateOf(80)),
                    SetUIState(mutableIntStateOf(10), mutableIntStateOf(90))
                )
            )
        )
    )
}


data class SetUIState(
    val numberOfReps: MutableState<Int> = mutableIntStateOf(0),
    val weight: MutableState<Int> = mutableIntStateOf(0)
)

data class ExerciseUIState(
    val name: MutableState<String> = mutableStateOf(""),
    val category: MutableState<Category> = mutableStateOf(Category.CAT1),
    val sets: SnapshotStateList<SetUIState>
)

data class RoutineViewUIState(
    val routineName: String,
    val exercises: SnapshotStateList<ExerciseUIState>
)