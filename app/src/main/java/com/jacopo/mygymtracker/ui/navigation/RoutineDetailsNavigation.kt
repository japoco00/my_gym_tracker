package com.jacopo.mygymtracker.ui.navigation

import android.util.Log
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.jacopo.mygymtracker.data.repositories.RoutineRepository
import com.jacopo.mygymtracker.ui.screen.RoutineScreen
import com.jacopo.mygymtracker.viewModel.RoutineViewModelFactory
import com.jacopo.mygymtracker.viewModel.RoutineViewVM
import kotlinx.serialization.Serializable

@Serializable
data class RoutineDetails(val routineId: Int)

fun NavGraphBuilder.routineDetailsDestination(
    routineRepository: RoutineRepository, onNavigateBack: () -> Unit = {}
) {
    composable<RoutineDetails>() { backStackEntry ->
        val routine = backStackEntry.toRoute<RoutineDetails>()
        val vm = viewModel<RoutineViewVM>(
            factory = RoutineViewModelFactory(
                routineId = if (routine.routineId == - 1) null else routine.routineId,
                routineRepository = routineRepository
            )
        )
        val uiState = vm.routineState
        Log.d(
            "RoutineDetailsNavigation",
            "Navigating to routine details with ID: ${routine.routineId}"
        )
        RoutineScreen(onBack = {
            vm.saveRoutine()
            onNavigateBack()
        }, uiState = uiState, onDeleteExercise = {
            vm.removeExercise(it)
        }, onAddNewSet = {
            vm.addNewSet(it)
        }, onAddEmptyExercise = {
            vm.addEmptyExercise()
        })
    }
}

fun NavController.navigateToRoutineDetails(routineId: Int) {
    navigate(route = RoutineDetails(routineId))
}
