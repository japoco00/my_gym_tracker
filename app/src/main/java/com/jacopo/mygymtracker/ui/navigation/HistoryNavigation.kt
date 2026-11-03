package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.jacopo.mygymtracker.ui.history.HistoryScreen
import com.jacopo.mygymtracker.ui.history.HistoryViewModel
import com.jacopo.mygymtracker.ui.history.WorkoutDetailScreen
import com.jacopo.mygymtracker.ui.history.WorkoutDetailViewModel
import kotlinx.serialization.Serializable

@Serializable
data object HistoryRoute

@Serializable
data class WorkoutDetailRoute(val sessionId: Int)

fun NavGraphBuilder.historyDestination(
    onOpenWorkoutDetail: (sessionId: Int) -> Unit,
) {
    composable<HistoryRoute> {
        val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        HistoryScreen(
            uiState = uiState,
            onWorkoutClick = onOpenWorkoutDetail,
        )
    }
}

fun NavGraphBuilder.workoutDetailDestination(
    onNavigateBack: () -> Unit,
    onOpenExerciseProgress: (exerciseId: Int) -> Unit,
) {
    composable<WorkoutDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<WorkoutDetailRoute>()
        val viewModel: WorkoutDetailViewModel = viewModel(factory = WorkoutDetailViewModel.factory(route.sessionId))
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(uiState.isClosed) {
            if (uiState.isClosed) onNavigateBack()
        }

        WorkoutDetailScreen(
            uiState = uiState,
            onBack = onNavigateBack,
            onEdit = viewModel::startEditing,
            onCancelEdit = viewModel::cancelEditing,
            onSaveEdits = viewModel::saveEdits,
            onDelete = viewModel::requestDelete,
            onConfirmDelete = viewModel::confirmDelete,
            onDismissDeleteDialog = viewModel::dismissDeleteDialog,
            onExerciseClick = onOpenExerciseProgress,
            onOpenAddExercises = viewModel::openAddExercisesSheet,
            onDismissAddExercises = viewModel::closeAddExercisesSheet,
            onConfirmAddExercises = viewModel::addExercises,
            onAddSet = viewModel::addSet,
            onOpenSetEditor = viewModel::openSetEditor,
            onCloseSetEditor = viewModel::closeSetEditor,
            onDeleteSet = viewModel::removeSet,
            onRepsChange = viewModel::updateSetReps,
            onWeightChange = viewModel::updateSetWeight,
            onDurationChange = viewModel::updateSetDuration,
        )
    }
}

fun NavController.navigateToWorkoutDetail(sessionId: Int) {
    navigate(route = WorkoutDetailRoute(sessionId = sessionId))
}

/** Pops the detail screen only, so Back and the end of a delete can't both pop. */
fun NavController.closeWorkoutDetail() {
    popBackStack<WorkoutDetailRoute>(inclusive = true)
}
