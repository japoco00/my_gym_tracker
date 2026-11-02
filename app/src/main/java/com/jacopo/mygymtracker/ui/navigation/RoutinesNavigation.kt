package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jacopo.mygymtracker.ui.routines.RoutinesListScreen
import com.jacopo.mygymtracker.ui.routines.RoutinesListViewModel
import kotlinx.serialization.Serializable

@Serializable
data object RoutinesRoute

fun NavGraphBuilder.routinesDestination(
    onCreateRoutine: () -> Unit,
    onOpenRoutine: (Int) -> Unit,
) {
    composable<RoutinesRoute> {
        val viewModel: RoutinesListViewModel = viewModel(factory = RoutinesListViewModel.Factory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        RoutinesListScreen(
            uiState = uiState,
            onCreateRoutine = onCreateRoutine,
            onRoutineClick = onOpenRoutine,
            onDeleteRoutine = viewModel::deleteRoutine,
        )
    }
}
