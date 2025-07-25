package com.jacopo.mygymtracker.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacopo.mygymtracker.AppContainer
import com.jacopo.mygymtracker.ui.components.RoutineOverviewCard
import com.jacopo.mygymtracker.viewModel.RoutinesListVM
import com.jacopo.mygymtracker.viewModel.RoutinesListVMFactory

@Composable
fun RoutinesListScreen(
    appContainer: AppContainer,
    onNavigateToDetails: (Int) -> Unit = { /* No-op */ },
) {

    val factory = remember {
        RoutinesListVMFactory(
            routineRepository = appContainer.routineRepository,
        )
    }
    val vm: RoutinesListVM = viewModel(factory = factory)
    val routines by vm.routines.collectAsState()


    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn {
            items(routines) { routine ->
                RoutineOverviewCard(routine, onClick = {onNavigateToDetails(routine.id!!)}, onDelete = { vm.deleteRoutine(routine) })
            }
        }
        Button(onClick = { vm.addEmptyRoutine() }) {
            Text(text = "Add Routine")
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Preview
@Composable
fun RoutinesListScreenPreview() {
//    RoutinesListScreen(
//        routines = listOf(
//            Routine(
//                id = 1,
//                name = "Sample Routine 1",
//                note = "This is a sample routine note.",
//                exercises = mutableStateListOf(
//                    Exercise(
//                        name = "Bench Press", sets = mutableStateListOf(
//                            ExerciseSet(reps = 10, weight = 10), ExerciseSet(reps = 8, weight = 12)
//                        ), id = 0, note = null, category = Category.CAT1
//                    ), Exercise(
//                        name = "Squat",
//                        sets = mutableStateListOf(
//                            ExerciseSet(reps = 3, weight = 5), ExerciseSet(reps = 2, weight = 4)
//                        ),
//                        id = null,
//                        note = null,
//                        category = Category.CAT3,
//                    )
//                )
//            ),
//            Routine(
//                id = 2,
//                name = "Sample Routine 2",
//                note = "This is another sample routine note.",
//                exercises = mutableListOf(
//                    Exercise(
//                        name = "Bench Press", sets = mutableStateListOf(
//                            ExerciseSet(reps = 10, weight = 10), ExerciseSet(reps = 8, weight = 12)
//                        ), id = 0, note = null, category = Category.CAT1
//                    ), Exercise(
//                        name = "Squat",
//                        sets = mutableStateListOf(
//                            ExerciseSet(reps = 3, weight = 5), ExerciseSet(reps = 2, weight = 4)
//                        ),
//                        id = null,
//                        note = null,
//                        category = Category.CAT3,
//                    )
//                )
//            )
//        )
//    )
}

