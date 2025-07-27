package com.jacopo.mygymtracker.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.jacopo.mygymtracker.ui.ExerciseUIState
import com.jacopo.mygymtracker.ui.components.ExerciseCard
import com.jacopo.mygymtracker.ui.components.SwipableElement
import com.jacopo.mygymtracker.viewModel.RoutineViewVM

@Composable
fun RoutineScreen(
    navController: NavHostController,
    vm: RoutineViewVM
) {

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
                    SwipableElement<ExerciseUIState>(
                        item = exercise,
                        onDismiss = { vm.removeExercise(it) },
                        content = {
                            ExerciseCard(
                                exercise = exercise,
                                addNewSet = { vm.addNewSet(exercise) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }
            Button(
                onClick = { vm.addEmptyExercise() },
                modifier = Modifier.padding(0.dp, 16.dp)
            ) {
                Text("Add Exercise")
            }
        }
    }
}
