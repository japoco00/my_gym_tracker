package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jacopo.mygymtracker.ui.components.LocalNavAnimatedVisibilityScope
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
    composable<RoutinesRoute>(
        // The tapped row grows into the editor, so the rest of the list only fades.
        exitTransition = { if (targetState.editsSavedRoutine()) fadeOut() else null },
        popEnterTransition = { if (initialState.editsSavedRoutine()) fadeIn() else null },
    ) {
        val viewModel: RoutinesViewModel = viewModel(factory = RoutinesViewModel.Factory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(uiState.startedWorkoutId) {
            uiState.startedWorkoutId?.let { sessionId ->
                onOpenWorkout(sessionId)
                viewModel.onWorkoutOpened()
            }
        }

        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
            RoutinesScreen(
                uiState = uiState,
                onCreateRoutine = onCreateRoutine,
                onRoutineClick = onOpenRoutine,
                onDeleteRoutine = viewModel::deleteRoutine,
                onUndoDelete = viewModel::undoDelete,
                onConfirmDelete = viewModel::confirmDelete,
                onStartWorkout = viewModel::startWorkout,
                onResumeWorkout = onOpenWorkout,
                onDiscardActiveAndStart = viewModel::discardActiveAndStart,
                onDismissStartConflict = viewModel::dismissStartConflict,
            )
        }
    }
}
