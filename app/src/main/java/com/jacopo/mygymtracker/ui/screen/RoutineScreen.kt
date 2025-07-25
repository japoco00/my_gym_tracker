package com.jacopo.mygymtracker.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jacopo.mygymtracker.AppContainer
import com.jacopo.mygymtracker.ui.components.ExerciseCard
import com.jacopo.mygymtracker.viewModel.RoutineViewModelFactory
import com.jacopo.mygymtracker.viewModel.RoutineViewVM

@Composable
fun RoutineScreen(
    navController: NavHostController,
    vm : RoutineViewVM
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
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn() {
                items(vm.routineState.value!!.exercises) { exercise ->
                    ExerciseCard(exercise, addNewSet = { vm.addNewSet(exercise) })
                }
                item {
                    Button(onClick = { vm.addEmptyExercise() }) {
                        Text("Add Exercise")
                    }
                }
            }
        }
    }
}
