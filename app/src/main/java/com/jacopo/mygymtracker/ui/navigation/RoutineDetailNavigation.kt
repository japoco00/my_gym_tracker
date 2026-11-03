package com.jacopo.mygymtracker.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.jacopo.mygymtracker.R
import com.jacopo.mygymtracker.ui.routines.RoutineDetailScreen
import com.jacopo.mygymtracker.ui.routines.RoutineDetailViewModel
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
        val untitledName = stringResource(R.string.routine_untitled)

        LaunchedEffect(uiState.isClosed) {
            if (uiState.isClosed) onNavigateBack()
        }

        RoutineDetailScreen(
            uiState = uiState,
            onBack = { viewModel.saveRoutine(untitledName) },
            onNameChange = viewModel::updateRoutineName,
            onNoteChange = viewModel::updateRoutineNote,
            onOpenAddExercises = viewModel::openAddExercisesSheet,
            onDismissAddExercises = viewModel::closeAddExercisesSheet,
            onConfirmAddExercises = viewModel::addCatalogExercises,
            onOpenNewExercise = viewModel::openNewExerciseSheet,
            onDismissNewExercise = viewModel::closeNewExerciseSheet,
            onConfirmNewExercise = viewModel::addNewExercise,
            onRemoveExercise = viewModel::removeExercise,
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

fun NavController.navigateToRoutineDetail(routineId: Int? = null) {
    navigate(route = RoutineDetailRoute(routineId = routineId))
}

/** Pops the editor only, so a repeated call can't pop the screen below it. */
fun NavController.closeRoutineDetail() {
    popBackStack<RoutineDetailRoute>(inclusive = true)
}
