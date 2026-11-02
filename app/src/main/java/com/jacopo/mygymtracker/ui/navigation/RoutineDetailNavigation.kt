package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.jacopo.mygymtracker.ui.routinedetail.RoutineDetailScreen
import com.jacopo.mygymtracker.ui.routinedetail.RoutineDetailViewModel
import kotlinx.serialization.Serializable

@Serializable
data class RoutineDetailRoute(val routineId: Int? = null)

fun NavGraphBuilder.routineDetailDestination(
    onNavigateBack: () -> Unit,
) {
    composable<RoutineDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<RoutineDetailRoute>()
        val viewModel: RoutineDetailViewModel = viewModel(
            factory = RoutineDetailViewModel.factory(route.routineId),
        )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        RoutineDetailScreen(
            uiState = uiState,
            onBack = { viewModel.saveRoutine(onSaved = onNavigateBack) },
            onNameChange = viewModel::updateRoutineName,
            onExerciseNameChange = viewModel::updateExerciseName,
            onExerciseCategoryChange = viewModel::updateExerciseCategory,
            onAddEmptyExercise = viewModel::addEmptyExercise,
            onOpenAddExercises = viewModel::openAddExercisesSheet,
            onDismissAddExercises = viewModel::closeAddExercisesSheet,
            onConfirmAddExercises = viewModel::addCatalogExercises,
            onDeleteExercise = viewModel::removeExercise,
            onAddSet = viewModel::addSet,
            onDeleteSet = viewModel::removeSet,
            onRepsChange = viewModel::updateSetReps,
            onWeightChange = viewModel::updateSetWeight,
        )
    }
}

fun NavController.navigateToRoutineDetail(routineId: Int? = null) {
    navigate(route = RoutineDetailRoute(routineId = routineId))
}
