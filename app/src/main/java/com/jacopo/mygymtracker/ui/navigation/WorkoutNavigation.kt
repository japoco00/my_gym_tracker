package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.jacopo.mygymtracker.ui.workout.WorkoutScreen
import com.jacopo.mygymtracker.ui.workout.WorkoutViewModel
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutRoute(val sessionId: Int)

/** Leaving the screen keeps the workout running; it ends only through Finish or Discard. */
fun NavGraphBuilder.workoutDestination(
    onNavigateBack: () -> Unit,
) {
    composable<WorkoutRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<WorkoutRoute>()
        val viewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModel.factory(route.sessionId))
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(uiState.isClosed) {
            if (uiState.isClosed) onNavigateBack()
        }

        WorkoutScreen(
            uiState = uiState,
            onBack = onNavigateBack,
            onFinish = viewModel::requestFinish,
            onDiscard = viewModel::requestDiscard,
            onOpenAddExercises = viewModel::openAddExercisesSheet,
            onDismissAddExercises = viewModel::closeAddExercisesSheet,
            onConfirmAddExercises = viewModel::addExercises,
            onRemoveExercise = viewModel::requestRemoveExercise,
            onAddSet = viewModel::addSet,
            onDeleteSet = viewModel::removeSet,
            onRepsChange = viewModel::updateSetReps,
            onWeightChange = viewModel::updateSetWeight,
            onToggleSet = viewModel::toggleSetCompleted,
            onConfirmFinish = viewModel::confirmFinish,
            onConfirmDiscard = viewModel::confirmDiscard,
            onConfirmRemoveExercise = viewModel::confirmRemoveExercise,
            onResolveRoutineUpdate = viewModel::resolveRoutineUpdate,
            onDismissDialog = viewModel::dismissDialog,
        )
    }
}

fun NavController.navigateToWorkout(sessionId: Int) {
    navigate(route = WorkoutRoute(sessionId = sessionId)) { launchSingleTop = true }
}

/** Pops the workout screen only, so a repeated call can't pop the screen below it. */
fun NavController.closeWorkout() {
    popBackStack<WorkoutRoute>(inclusive = true)
}
