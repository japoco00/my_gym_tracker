package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jacopo.mygymtracker.ui.routines.RoutinesScreen
import com.jacopo.mygymtracker.ui.routines.RoutinesViewModel
import kotlinx.serialization.Serializable

@Serializable
data object RoutinesRoute

fun NavGraphBuilder.routinesDestination(
    onCreateRoutine: () -> Unit,
    onOpenRoutine: (Int) -> Unit,
    onOpenWorkout: (sessionId: Int) -> Unit,
) {
    composable<RoutinesRoute> {
        val viewModel: RoutinesViewModel = viewModel(factory = RoutinesViewModel.Factory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        RoutinesScreen(
            uiState = uiState,
            onCreateRoutine = onCreateRoutine,
            onRoutineClick = onOpenRoutine,
            onDeleteRoutine = viewModel::deleteRoutine,
            onUndoDelete = viewModel::undoDelete,
            onConfirmDelete = viewModel::confirmDelete,
            onStartWorkout = { routine -> viewModel.startWorkout(routine, onStarted = onOpenWorkout) },
            onResumeWorkout = onOpenWorkout,
            onDiscardActiveAndStart = { viewModel.discardActiveAndStart(onStarted = onOpenWorkout) },
            onDismissStartConflict = viewModel::dismissStartConflict,
        )
    }
}
