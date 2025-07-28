package com.jacopo.mygymtracker.ui.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jacopo.mygymtracker.data.repositories.RoutineRepository
import com.jacopo.mygymtracker.ui.screen.RoutinesListScreen
import com.jacopo.mygymtracker.viewModel.RoutinesListVM
import com.jacopo.mygymtracker.viewModel.RoutinesListVMFactory
import kotlinx.serialization.Serializable

@Serializable
object Routines

fun NavGraphBuilder.routinesDestination(
    routineRepository: RoutineRepository,
    onNavigateToRoutineDetails: (Int) -> Unit = { /* No-op */ }
) {
    composable<Routines> {
        val vm = viewModel<RoutinesListVM>(
            factory = RoutinesListVMFactory(
                routineRepository = routineRepository
            )
        )
        RoutinesListScreen(
            onNavigateToRoutineDetails = { routineId ->
                onNavigateToRoutineDetails(routineId)
            }, onDeleteRoutine = { routine ->
                vm.deleteRoutine(routine)
            }, routinesFlow = vm.routines
        )
    }
}
