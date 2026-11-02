package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.jacopo.mygymtracker.ui.progress.ExerciseProgressScreen
import com.jacopo.mygymtracker.ui.progress.ExerciseProgressViewModel
import kotlinx.serialization.Serializable

@Serializable
data class ExerciseProgressRoute(val exerciseId: Int)

fun NavGraphBuilder.exerciseProgressDestination(
    onNavigateBack: () -> Unit,
) {
    composable<ExerciseProgressRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ExerciseProgressRoute>()
        val viewModel: ExerciseProgressViewModel = viewModel(factory = ExerciseProgressViewModel.factory(route.exerciseId))
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        ExerciseProgressScreen(
            uiState = uiState,
            onBack = onNavigateBack,
            onMetricSelect = viewModel::selectMetric,
        )
    }
}

fun NavController.navigateToExerciseProgress(exerciseId: Int) {
    navigate(route = ExerciseProgressRoute(exerciseId = exerciseId))
}

/** Pops the progress screen only, so a double tap on Back can't also pop the screen below. */
fun NavController.closeExerciseProgress() {
    popBackStack<ExerciseProgressRoute>(inclusive = true)
}
