package com.jacopo.mygymtracker.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.jacopo.mygymtracker.ui.components.ExerciseCard
import com.jacopo.mygymtracker.ui.components.SwipableCard
import com.jacopo.mygymtracker.viewModel.RoutineViewVM

@Composable
fun RoutineScreen(
    navController: NavHostController,
    vm: RoutineViewVM
) {

//    val factory = remember {
//        RoutineViewModelFactory(
//            routineId = if (routineId == -1) null else routineId,
//            routineRepository = appContainer.routineRepository,
//        )
//    }
//    val vm: RoutineViewVM = viewModel(factory = factory)
    BackHandler {
        vm.saveRoutine()
        navController.popBackStack()
    }

    if (vm.routineState.value == null) {
        CircularProgressIndicator()
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                items(
                    vm.routineState.value!!.exercises,
                    key = { it.id ?: it.draftId }
                    ) { exercise ->
                    SwipableCard(
                        exercise = exercise,
                        addNewSet = { vm.addNewSet(exercise)},
                        onRemove = { vm.removeExercise(exercise) },
                        modifier = Modifier.animateItem()
                        )
                }
            }
            Button(onClick = { vm.addEmptyExercise(vm.routineState.value!!.exercises.minOfOrNull { it.draftId }) },
                modifier = Modifier.padding(0.dp, 16.dp)) {
                Text("Add Exercise")
            }
        }
    }
}
